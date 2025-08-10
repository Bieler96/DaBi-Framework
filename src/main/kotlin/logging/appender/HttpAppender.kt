package logging.appender

import logging.LogLevel
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Appender zur Übermittlung von Logs an einen HTTP-Endpunkt.
 * Sendet Logs als JSON im Request-Body an die angegebene URL.
 */
class HttpAppender(private val url: String) : Appender {
	private val client = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build()

	override fun append(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
		try {
			val logData = buildJsonPayload(level, tag, message, throwable)
			val request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(logData))
				.timeout(Duration.ofSeconds(5))
				.build()

			client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.exceptionally { e ->
					System.err.println("Fehler beim Senden des Logs: ${e.message}")
					null
				}
		} catch (e: Exception) {
			System.err.println("Fehler beim Erstellen des HTTP-Requests: ${e.message}")
		}
	}

	private fun buildJsonPayload(level: LogLevel, tag: String, message: String, throwable: Throwable?): String {
		val timestamp = System.currentTimeMillis()
		val stackTrace = throwable?.stackTraceToString() ?: ""

		return """{
		            "timestamp": $timestamp,
		            "level": "${level.name}",
		            "tag": "$tag",
		            "message": "${message.replace("\"", "\\\"")}",
		            ${if (throwable != null) "\"stackTrace\": \"${stackTrace.replace("\"", "\\\"")}\"" else ""}
		        }""".trimIndent()
	}
}
