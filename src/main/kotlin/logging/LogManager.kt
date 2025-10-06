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

		val appenders = mutableListOf<Appender>()

		config.appenders.forEach { appenderType ->
			when (appenderType) {
				LoggerConfig.AppenderType.CONSOLE -> appenders.add(ConsoleAppender())

				LoggerConfig.AppenderType.FILE -> {
					val logFile = File("application.log")
					appenders.add(FileAppender(logFile))
				}

				LoggerConfig.AppenderType.JSON_FILE -> {
					val logFile = File("application.json")
					appenders.add(JsonFileAppender(logFile.absolutePath))
				}

				LoggerConfig.AppenderType.HTTP -> {
					if (config.httpEndpoint == null) throw IllegalArgumentException("HTTP Appender requires an endpoint URL.")
					appenders.add(HttpAppender(config.httpEndpoint))
				}
			}
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
					appenders = listOf(LoggerConfig.AppenderType.CONSOLE)
				)
			)
		}

		return logger
	}
}