package utils.ktor

import de.bieler.ktor.exception.UnexpectedStatusException
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import logging.LogManager
import kotlin.time.measureTimedValue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StatusHandlerScope<T> {
	val handlers: MutableMap<HttpStatusCode, suspend (HttpResponse) -> T> = mutableMapOf()
	private var fallbackHandler: (suspend (HttpStatusCode, String) -> T)? = null

	fun on(status: HttpStatusCode, block: suspend (HttpResponse) -> T) {
		handlers[status] = block
	}

	fun onElse(block: suspend (HttpStatusCode, String) -> T) {
		fallbackHandler = block
	}

	fun getFallback(): (suspend (HttpStatusCode, String) -> T)? = fallbackHandler
}

suspend inline fun <reified T> HttpClient.getWithHandlers(
	url: String,
	log: Boolean = false,
	crossinline block: StatusHandlerScope<T>.() -> Unit
): T {
	val logger = LogManager.getLogger()
	val tag = "getWithHandlers"
	val scope = StatusHandlerScope<T>().apply(block)
	val startTime = System.currentTimeMillis()

	if (log) {
		logger.d(tag, "➡️  GET $url")
		logger.d(tag, "🔍 Request initiiert um ${java.time.LocalDateTime.now()}")
	}

	val response = get(url)
	val status = response.status
	val handler = scope.handlers[status]
	val endTime = System.currentTimeMillis()
	val duration = endTime - startTime

	if (log) {
		logger.d(tag, "⬅️  ${status.value} ${status.description} (${duration}ms)")
		logger.d(tag, "📋 Response Headers: ${response.headers.entries().joinToString(", ") { "${it.key}: ${it.value}" }}")
	}

	return when {
		handler != null -> {
			if (log) {
				logger.d(tag, "✅ Verarbeitung mit registriertem Handler für Status ${status.value}")
				logger.d(tag, "⏱️ Antwort erhalten nach ${duration}ms")
				logger.d(tag, "🧩 Content-Type: ${response.headers["Content-Type"]}")
				logger.d(tag, "📦 Content-Length: ${response.headers["Content-Length"]} bytes")

				// Logge den Response-Body
				try {
					val responseBody = response.bodyAsText()
					if (responseBody.length > 1000) {
						logger.d(tag, "📝 Response-Body (gekürzt): ${responseBody.take(1000)}...")
						logger.d(tag, "📊 Vollständige Body-Länge: ${responseBody.length} Zeichen")
					} else {
						logger.d(tag, "📝 Response-Body: $responseBody")
					}
				} catch (e: Exception) {
					logger.w(tag, "⚠️ Konnte Response-Body nicht lesen: ${e.message}")
				}
			}
			try {
				val handlerStartTime = System.currentTimeMillis()
				val result = handler(response)
				if (log) {
					val handlerDuration = System.currentTimeMillis() - handlerStartTime
					logger.d(tag, "🏁 Handler abgeschlossen in ${handlerDuration}ms")
				}
				result
			} catch (e: Exception) {
				if (log) {
					logger.e(tag, "💥 Fehler im Handler: ${e.message}")
					e.stackTrace?.firstOrNull()?.let { frame ->
						logger.e(tag, "   bei ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
					}
				}
				throw e
			}
		}
		scope.getFallback() != null -> {
			val body = response.bodyAsText()
			if (log) {
				logger.d(tag, "🔄 Verwende Fallback-Handler für Status ${status.value}")
				logger.d(tag, "📄 Body (fallback): $body")
				if (body.length > 500) logger.d(tag, "⚠️ Body gekürzt (${body.length} Zeichen)")
				logger.d(tag, "⏱️ Zeit bis Fallback: ${duration}ms")
			}
			try {
				val fallbackStartTime = System.currentTimeMillis()
				val result = scope.getFallback()!!.invoke(status, body)
				if (log) {
					val fallbackDuration = System.currentTimeMillis() - fallbackStartTime
					logger.d(tag, "🏁 Fallback-Handler abgeschlossen in ${fallbackDuration}ms")
				}
				result
			} catch (e: Exception) {
				if (log) {
					logger.e(tag, "💥 Fehler im Fallback-Handler: ${e.message}")
					e.stackTrace?.firstOrNull()?.let { frame ->
						logger.e(tag, "   bei ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
					}
				}
				throw e
			}
		}

		else -> {
			val body = response.bodyAsText()
			if (log) {
				logger.w(tag, "❗ Unerwarteter Status ${status.value} nach ${duration}ms")
				logger.w(tag, "📋 Response Headers: ${response.headers.entries().joinToString(", ") { "${it.key}: ${it.value}" }}")
				logger.w(tag, "📄 Body: $body")
			}
			throw UnexpectedStatusException(status, body)
		}
	}
}