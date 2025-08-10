package de.bieler.dabilogger.core

import core.appender.JsonFileAppender
import de.bieler.dabilogger.core.appender.Appender
import de.bieler.dabilogger.core.appender.ConsoleAppender
import de.bieler.dabilogger.core.appender.FileAppender
import de.bieler.dabilogger.core.appender.HttpAppender
import java.io.File

object LogManager {
	private lateinit var logger: Logger
	private var isInitialized = false

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