package eventqueue

import eventqueue.exception.EventQueueTimeoutException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * An idiomatic Kotlin event queue using Coroutines and Flows.
 *
 * This queue processes events asynchronously based on priority. It supports timeouts, retries,
 * and custom transformations or filtering logic provided as lambdas.
 *
 * @param T The type of events to be processed.
 * @param globalTimeoutMillis The default timeout for events in milliseconds.
 * @param coroutineScope The CoroutineScope in which to run the event processing. Defaults to a scope with Dispatchers.Default.
 * @param transformer A suspendable lambda to transform an event before processing.
 * @param filter A suspendable lambda to filter an event. If it returns false, the event is discarded.
 */
class EventQueue<T>(
    private val globalTimeoutMillis: Long = 5000,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val transformer: suspend (T) -> T = { it },
    private val filter: suspend (T) -> Boolean = { true }
) {
    private val queue = PriorityQueue<PrioritizedEvent<T>>()
    private val mutex = Mutex()
    private val newEventSignal = Channel<Unit>(Channel.CONFLATED)

    private val _processedEvents = MutableSharedFlow<T>()
    val processedEvents: SharedFlow<T> = _processedEvents.asSharedFlow()

    val deadLetterQueue = mutableListOf<PrioritizedEvent<T>>()

    // Monitoring
    private val processedCount = AtomicInteger(0)
    private val discardedCount = AtomicInteger(0)
    private val retriedCount = AtomicInteger(0)
    private val totalProcessingTime = AtomicLong(0)
    private val totalWaitTime = AtomicLong(0)

    init {
        startEventProcessor()
    }

    private fun startEventProcessor() {
        coroutineScope.launch {
            for (signal in newEventSignal) {
                while (coroutineContext.isActive) {
                    val event = getNextEvent() ?: break
                    processEvent(event) // Process sequentially
                }
            }
        }
    }

    private suspend fun getNextEvent(): PrioritizedEvent<T>? = mutex.withLock {
        queue.poll()
    }

    /**
     * Enqueues an event and waits for its processing to complete.
     *
     * @param event The event to enqueue.
     * @param priority The priority of the event. Higher numbers are processed first.
     * @param timeoutMillis Timeout for this specific event.
     * @param retries Number of retries upon failure.
     * @return A Result object indicating success or failure.
     */
    suspend fun enqueue(
        event: T,
        priority: Int = 0,
        timeoutMillis: Long = globalTimeoutMillis,
        retries: Int = 3
    ): Result<T> {
        val resultDeferred = CompletableDeferred<Result<T>>()
        val prioritizedEvent = PrioritizedEvent(priority, event, timeoutMillis, retries, resultDeferred)

        mutex.withLock {
            queue.add(prioritizedEvent)
        }
        newEventSignal.send(Unit)

        return resultDeferred.await()
    }

    private suspend fun processEvent(event: PrioritizedEvent<T>) {
        val startTime = System.currentTimeMillis()
        totalWaitTime.addAndGet(startTime - event.timestamp)

        try {
            withTimeoutOrNull(event.timeoutMillis) {
                val transformedEvent = transformer(event.event)
                if (filter(transformedEvent)) {
                    _processedEvents.emit(transformedEvent)
                    event.result.complete(Result.success(transformedEvent))
                    processedCount.incrementAndGet()
                } else {
                    event.result.complete(Result.failure(Exception("Event filtered out")))
                    discardedCount.incrementAndGet()
                }
            } ?: handleFailure(event, EventQueueTimeoutException("Event timed out after ${event.timeoutMillis}ms"))
        } catch (e: Exception) {
            handleFailure(event, e)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            totalProcessingTime.addAndGet(duration)
        }
    }

    private suspend fun handleFailure(event: PrioritizedEvent<T>, exception: Exception) {
        if (event.retriesLeft > 0) {
            retriedCount.incrementAndGet()
            // Re-queue with the same result deferred. The original caller will wait for the retry.
            val retryEvent = event.copy(retriesLeft = event.retriesLeft - 1)
            mutex.withLock {
                queue.add(retryEvent)
            }
            newEventSignal.send(Unit)
        } else {
            discardedCount.incrementAndGet()
            deadLetterQueue.add(event)
            event.result.complete(Result.failure(exception))
        }
    }

    /**
     * Returns a map of the current queue statistics.
     */
    fun getStats(): Map<String, Number> {
        val processed = processedCount.get()
        val avgProcessingTime = if (processed > 0) totalProcessingTime.get() / processed else 0
        val avgWaitTime = if (processed > 0) totalWaitTime.get() / processed else 0

        return mapOf(
            "processed" to processed,
            "discarded" to discardedCount.get(),
            "retried" to retriedCount.get(),
            "deadLetterQueueSize" to deadLetterQueue.size,
            "averageProcessingTimeMs" to avgProcessingTime,
            "averageWaitTimeMs" to avgWaitTime
        )
    }

    /**
     * Shuts down the event queue and cancels any ongoing processing.
     */
    fun shutdown() {
        newEventSignal.close()
        coroutineScope.cancel()
    }
}