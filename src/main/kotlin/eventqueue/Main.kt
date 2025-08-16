package eventqueue

import kotlinx.coroutines.*
import kotlin.random.Random

fun main() = runBlocking {
    // 1. EventQueue erstellen
    println("1. EventQueue erstellen")
    val eventQueue = EventQueue<String>(
        globalTimeoutMillis = 3000, // Standard-Timeout
        // Optional: Transformer- und Filter-Lambdas übergeben
        transformer = { it.uppercase() },
        filter = { it.isNotEmpty() }
    )

    // 2. Abonniere den Strom der verarbeiteten Events
    println("2. Abonniere den Strom der verarbeiteten Events")
    val collectorJob = launch {
        eventQueue.processedEvents.collect { event ->
            println("Verarbeitetes Event empfangen: $event")
        }
    }

    // 3. Füge Events zur Warteschlange hinzu und warte auf die Ergebnisse
    println("3. Füge Events zur Warteschlange hinzu")

    val enqueueJobs = List(10) { i ->
        launch {
            val priority = Random.nextInt(0, 5)
            val timeout = 2000L

            println("Event-$i mit Priorität $priority wird hinzugefügt...")
            val result = eventQueue.enqueue(
                event = "Event-$i",
                priority = priority,
                timeoutMillis = timeout,
                retries = 2
            )
            result.onSuccess {
                println("Ergebnis für Event-$i: Erfolgreich ($it)")
            }.onFailure {
                println("Ergebnis für Event-$i: Fehlgeschlagen (${it.message})")
            }
        }
    }

    // Warte bis alle Events hinzugefügt wurden
    enqueueJobs.joinAll()

    // 4. Kritisches Event hinzufügen
    println("\n4. Kritisches Event hinzufügen")
    launch {
        val result = eventQueue.enqueue(
            event = "Critical Event!",
            priority = 10, // Höchste Priorität
            timeoutMillis = 1000
        )
        result.onSuccess {
            println("Ergebnis für Kritisches Event: Erfolgreich ($it)")
        }.onFailure {
            println("Ergebnis für Kritisches Event: Fehlgeschlagen (${it.message})")
        }
    }


    // Ein wenig warten, damit einige Events verarbeitet werden können
    println("\nWarte 5 Sekunden, damit die Verarbeitung stattfinden kann...")
    delay(5000)

    // 5. Statistiken ausgeben
    println("\n5. Statistiken ausgeben")
    val stats = eventQueue.getStats()
    stats.forEach { (key, value) ->
        println("$key: $value")
    }

    // 6. Dead Letter Queue überprüfen
    println("\n6. Dead Letter Queue überprüfen")
    println("Inhalt der Dead Letter Queue:")
    eventQueue.deadLetterQueue.forEach { deadEvent ->
        println("Verworfenes Event: ${deadEvent.event} (Prio: ${deadEvent.priority})")
    }

    // 7. Verarbeitung stoppen
    println("\n7. Verarbeitung stoppen")
    eventQueue.shutdown()
    collectorJob.cancelAndJoin() // Den Collector-Job auch beenden
    println("Event-Verarbeitung beendet.")
}
