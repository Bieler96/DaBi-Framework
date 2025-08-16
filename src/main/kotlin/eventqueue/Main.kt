package eventqueue

import kotlinx.coroutines.*
import kotlin.random.Random

fun main() = runBlocking {
	// 1. EventQueue erstellen
	println("1. EventQueue erstellen")
	val eventQueue = EventQueue<String>(
		globalTimeoutMillis = 3000, // Standard-Timeout
		batchSize = 5              // Events pro Batch
	)

	// 2. Abonniere den Event-Stream
	println("2. Abonniere den Event-Stream")
	val job = launch {
		eventQueue.events.collect { event ->
			println("Verarbeitetes Event empfangen: $event")
		}
	}

	// 3. Füge Events zur Warteschlange hinzu
	println("3. Füge Events zur Warteschlange hinzu")
	val deferredResults = mutableListOf<CompletableDeferred<Result<String>>>()

	repeat(10) { i ->
		val priority = Random.nextInt(0, 5) // Zufällige Priorität
		val retries = 2                     // Max. 2 Wiederholungen
		val timeout = 2000L                 // 2 Sekunden Timeout

		val result = eventQueue.enqueue(
			event = "Event-$i",
			priority = priority,
			timeoutMillis = timeout,
			retries = retries
		)
		deferredResults.add(result)
		println("Event-$i mit Priorität $priority enqueued.")
	}

	// 4. Warte auf alle Ergebnisse und zeige Rückmeldungen
	println("4. Warte auf alle Ergebnisse und zeige Rückmeldungen")
	deferredResults.forEachIndexed { index, result ->
		val status = result.await()
		status.onSuccess {
			println("Event-$index erfolgreich verarbeitet: $it")
		}.onFailure {
			println("Event-$index fehlgeschlagen: ${it.message}")
		}
	}

	// 5. Kritisches Event hinzufügen
	println("5. Kritisches Event hinzufügen")
	eventQueue.enqueue(
		event = "Critical Event!",
		priority = 10,
		timeoutMillis = 1000 // Kürzeres Timeout
	)

	// 6. Statistiken ausgeben
	println("6. Statistiken ausgeben")
	println("Warte 3 Sekunden, bevor Statistiken ausgegeben werden...")
	delay(3000)
	eventQueue.logStats()

	// 7. Dead Letter Queue überprüfen
	println("7. Dead Letter Queue überprüfen")
	println("Dead Letter Queue Inhalte:")
	eventQueue.deadLetterQueue.forEach { deadEvent ->
		println("Verworfenes Event: $deadEvent")
	}

	// 8. Verarbeitung stoppen
	println("8. Verarbeitung stoppen")
	eventQueue.stopProcessing()
	job.cancelAndJoin()
	println("Event-Verarbeitung beendet.")
}
