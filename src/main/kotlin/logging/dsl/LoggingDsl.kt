package logging.dsl

import logging.LogLevel
import logging.LogManager
import logging.LoggerConfig

@DslMarker
annotation class LoggingDslMarker

@LoggingDslMarker
class LoggingDsl {
	var minLogLevel: LogLevel = LogLevel.INFO
	private val appenders = mutableListOf<LoggerConfig.AppenderType>()
	private var httpEndpoint: String? = null
	private var logFile: String = "application.log"
	private var jsonLogFile: String = "application.json"

	fun appenders(block: AppendersDsl.() -> Unit) {
		val appendersDsl = AppendersDsl().apply(block)
		appenders.addAll(appendersDsl.appenders)
		httpEndpoint = appendersDsl.httpEndpoint
		logFile = appendersDsl.logFile
		jsonLogFile = appendersDsl.jsonLogFile
	}

	internal fun build(): LoggerConfig {
		return LoggerConfig(
			minLogLevel = minLogLevel,
			appenders = appenders,
			httpEndpoint = httpEndpoint,
			logFile = logFile,
			jsonLogFile = jsonLogFile
		)
	}
}

@LoggingDslMarker
class AppendersDsl {
	internal val appenders = mutableListOf<LoggerConfig.AppenderType>()
	internal var httpEndpoint: String? = null
	internal var logFile: String = "application.log"
	internal var jsonLogFile: String = "application.json"

	fun console() {
		appenders.add(LoggerConfig.AppenderType.CONSOLE)
	}

	fun file(fileName: String = "application.log") {
		appenders.add(LoggerConfig.AppenderType.FILE)
		logFile = fileName
	}

	fun json(fileName: String = "application.json") {
		appenders.add(LoggerConfig.AppenderType.JSON_FILE)
		jsonLogFile = fileName
	}

	fun http(endpoint: String) {
		appenders.add(LoggerConfig.AppenderType.HTTP)
		httpEndpoint = endpoint
	}
}

/**
 * Configures the logging framework using a DSL.
 *
 * Example:
 * ```kotlin
 * logging {
 *     minLogLevel = LogLevel.DEBUG
 *     appenders {
 *         console()
 *         file("my-app.log")
 *         json("my-app.json")
 *         http("http://localhost:8080/logs")
 *     }
 * }
 * ```
 */
fun logging(block: LoggingDsl.() -> Unit) {
	val config = LoggingDsl().apply(block).build()
	LogManager.initialize(config)
}
