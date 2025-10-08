package logging

import logging.appender.JsonFileAppender
import logging.appender.Appender
import logging.appender.ConsoleAppender
import logging.appender.FileAppender
import logging.appender.HttpAppender
import java.io.File

object LogManager {
	internal lateinit var logger: Logger
	internal var isInitialized = false

	internal fun resetForTesting() {
		isInitialized = false
		// logger is lateinit, so we can't set it to null directly.
		// If it was initialized, we should shut it down.
		if (this::logger.isInitialized) {
			logger.shutdown()
		}
	}

	fun initialize(config: LoggerConfig) {
		if (isInitialized) return

		val appenders = mutableListOf<AppenderWithLevel>()

		config.appenders.forEach { appenderConfig ->
			val appender = when (appenderConfig.type) {
				LoggerConfig.AppenderType.CONSOLE -> ConsoleAppender()
				LoggerConfig.AppenderType.FILE -> {
					val logFile = File(config.logFile)
					FileAppender(logFile)
				}

				LoggerConfig.AppenderType.JSON_FILE -> {
					val logFile = File(config.jsonLogFile)
					JsonFileAppender(logFile.absolutePath)
				}

				LoggerConfig.AppenderType.HTTP -> {
					if (config.httpEndpoint == null) throw IllegalArgumentException("HTTP Appender requires an endpoint URL.")
					HttpAppender(config.httpEndpoint)
				}
			}
			appenders.add(AppenderWithLevel(appender, appenderConfig.minLogLevel))
		}

		logger = Logger(appenders, config.minLogLevel)
		isInitialized = true

		Runtime.getRuntime().addShutdownHook(Thread {
			println("Shutting down logger...")
			logger.shutdown()
		})
	}

	fun getLogger(): Logger {
		if (!this::logger.isInitialized) {
			initialize(
				LoggerConfig(
					minLogLevel = LogLevel.DEBUG,
					appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.CONSOLE))
				)
			)
		}

		return logger
	}
}