package eventqueue

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

/**
 * A class representing an event queue with prioritization and timeout handling.
 *
 * This class allows enqueuing events with a specified priority and timeout.
 * It processes events in the order of their priority and discards events that have timed out.
 * The class also provides methods to transform and filter events before emitting them.
 * It maintains statistics on the number of processed and discarded events, as well as the total processing time.
 *
 * @param T The type of events to be processed.
 * @param globalTimeoutMillis The default timeout for events in milliseconds.
 * @param batchSize The number of events to process in a batch.
 */
class EventQueue<T>(
	private val globalTimeoutMillis: Long = 5000,
	private val batchSize: Int = 10,
) {
	private val subQueues = mutableMapOf<Int, PriorityQueue<PrioritizedEvent<T>>>()
	private val _events = MutableSharedFlow<T>()
	val events: SharedFlow<T> = _events
	val deadLetterQueue = mutableListOf<T>()
	private val customDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
	private val processorScope = CoroutineScope(customDispatcher)

	// monitoring
	private val processedCount = AtomicInteger(0)
	private val discardedCount = AtomicInteger(0)
	private val retriedCount = AtomicInteger(0)
	private val totalProcessingTime = AtomicLong(0)
	private val totalWaitTime = AtomicLong(0)
	private val mutex = Mutex()
	private val batchQueue = mutableListOf<PrioritizedEvent<T>>()

	/**
	 * Represents a prioritized event with a timestamp and a timeout.
	 */
	private var processorJob = processorScope.launch {
		while (isActive) {
			processBatch(stopWhenEmpty = true)  // Stoppe, wenn keine Events mehr da sind
		}
	}

	/**
	 * Enqueues an event with a given priority and timeout.
	 *
	 * @param event The event to be enqueued.
	 * @param priority The priority of the event (default is 0).
	 * @param timeoutMillis The timeout for the event in milliseconds (default is globalTimeoutMillis).
	 */
	suspend fun enqueue(
		event: T,
		priority: Int = 0,
		timeoutMillis: Long = globalTimeoutMillis,
		retries: Int = 3
	): CompletableDeferred<Result<T>> {
		val result = CompletableDeferred<Result<T>>()
		val prioritizedEvent = PrioritizedEvent(priority, event, timeoutMillis, retries, result)

		mutex.withLock {
			// Prüfe, ob die Warteschlange für diese Priorität existiert und Events hinzufügt
			val queue = subQueues.getOrPut(priority) { PriorityQueue() }
			queue.add(prioritizedEvent)
//			println("Event enqueued mit Priorität $priority: $event")  // Debugging
		}

		// Start the processing if it was stopped
		if (processorJob.isCompleted || processorJob.isCancelled) {
			startProcessing()
		}

		return result
	}

	private fun startProcessing() {
		if (processorJob.isCompleted || processorJob.isCancelled) {
//			println("Starte Verarbeitung...")
			processorJob = processorScope.launch {
				while (isActive) {
					processBatch()
				}
			}
		} else {
//			println("Verarbeitung läuft bereits.")
		}
	}

	private suspend fun processBatch(stopWhenEmpty: Boolean = false) {
		val batch = mutex.withLock {
			// Debugging: Überprüfe, ob Events in den Warteschlangen vorhanden sind
			val highestPriority = subQueues.keys.maxOrNull()
//			println("Überprüfe Warteschlange mit Priorität $highestPriority")  // Debugging

			val queue = highestPriority?.let { subQueues[it] }
			val events = queue?.take(batchSize) ?: emptyList()

			if (events.isEmpty()) {
//				println("Keine Events zum Verarbeiten in Warteschlange mit Priorität $highestPriority.")  // Debugging
			}

			batchQueue.clear()
			batchQueue.addAll(events)
			events
		}

		if (batch.isEmpty()) {
			if (stopWhenEmpty) {
//				println("Keine Events zum Verarbeiten. Verarbeitung wird gestoppt.")  // Debugging
				stopProcessing()
			} else {
//				println("Keine Events zum Verarbeiten. Warten...")  // Debugging
				delay(1000)
			}
			return
		}

//		println("Verarbeite Batch mit ${batch.size} Events.")  // Debugging

		for (event in batch) {
//			println("Verarbeite Event: $event")  // Debugging
			processEvent(event)
		}

		delay(100)
	}

	private suspend fun processEvent(event: PrioritizedEvent<T>) {
		val startTime = System.currentTimeMillis()
		try {
			val transformedEvent = transformEvent(event.event)
			if (filterEvent(transformedEvent)) {
				_events.emit(transformedEvent)
				event.result.complete(Result.success(transformedEvent))
				processedCount.incrementAndGet()
			} else {
//				println("Ereignis gefiltert: $transformedEvent")
			}
		} catch (e: Exception) {
			handleFailure(event, e)
		} finally {
			val duration = System.currentTimeMillis() - startTime
			totalProcessingTime.addAndGet(duration)
			totalWaitTime.addAndGet(System.currentTimeMillis() - event.timestamp)
		}
	}

	private suspend fun handleFailure(event: PrioritizedEvent<T>, exception: Exception) {
//		println("Ereignis fehlgeschlagen: ${event.event}, Fehler: ${exception.message}")
		if (event.retriesLeft > 0) {
			retriedCount.incrementAndGet()
			enqueue(event.event, event.priority, event.timeoutMillis, event.retriesLeft - 1)
		} else {
			deadLetterQueue.add(event.event)

			if (deadLetterQueue.size > 50) {
//				println("Warnung: Die Dead Letter Queue enthält viele Einträge (${deadLetterQueue.size}).")
			}

			event.result.complete(Result.failure(exception))
		}
	}

	/**
	 * Transforms an event.
	 * Converts strings to uppercase.
	 *
	 * @param event The event to be transformed.
	 * @return The transformed event.
	 */
	private fun transformEvent(event: T): T {
		return if (event is String) event.uppercase() as T else event
	}

	/**
	 * Filters an event.
	 * Filters out empty strings.
	 *
	 * @param event The event to be filtered.
	 * @return True if the event passes the filter, false otherwise.
	 */
	private fun filterEvent(event: T): Boolean {
		return if (event is String) event.isNotEmpty() else true
	}

	/**
	 * Logs the statistics of the event processing.
	 */
	fun logStats() {
		println("Verarbeitete Ereignisse: ${processedCount.get()}")
		println("Verworfene Ereignisse: ${discardedCount.get()}")
		println("Erneut versuchte Ereignisse: ${retriedCount.get()}")
		println("Dead Letter Queue: ${deadLetterQueue.size}")
		println(
			"Durchschnittliche Verarbeitungszeit: ${
				totalProcessingTime.get() / (processedCount.get().coerceAtLeast(1))
			} ms"
		)
		println(
			"Durchschnittliche Wartezeit: ${
				totalWaitTime.get() / (processedCount.get().coerceAtLeast(1))
			} ms"
		)
	}

	/**
	 * Stops the event processing.
	 */
	fun stopProcessing() {
		if (subQueues.values.all { it.isEmpty() }) {
			processorJob.cancel()
//			println("Verarbeitung gestoppt, da alle Warteschlangen leer sind.")
		} else {
//			println("Warteschlangen sind nicht leer, Verarbeitung wird fortgesetzt.")
		}
	}

	private fun <T> PriorityQueue<T>.take(n: Int): List<T> {
		val list = mutableListOf<T>()
		repeat(n) {
			if (isNotEmpty()) list.add(poll())
		}
		return list
	}

	private fun calculateTimeout(event: T): Long {
		return when (event) {
			is CriticalEvent -> 1000L // Kürzeres Timeout für kritische Events
			else -> globalTimeoutMillis
		}
	}

	private fun dynamicPriority(event: PrioritizedEvent<T>): Int {
		return when (event.event) {
			is CriticalEvent -> 10
			else -> event.priority
		}
	}
}