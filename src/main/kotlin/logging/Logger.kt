package logging

import logging.appender.Appender
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

data class AppenderWithLevel(val appender: Appender, val minLogLevel: LogLevel?)

class Logger(
	private val appenders: List<AppenderWithLevel>,
	@Volatile private var minLogLevel: LogLevel = LogLevel.INFO
) : LoggerBase {
	private val logQueue = LinkedBlockingQueue<LogEvent>()
	private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
		Thread(r, "dabi-logger-thread").apply {
			isDaemon = true
		}
	}

	companion object {
		private val POISON_PILL = LogEvent(LogLevel("POISON", -1), "", null)
	}

	data class LogEvent(
		val level: LogLevel,
		val message: String,
		val throwable: Throwable?
	)

	init {
		logExecutor.submit {
			try {
				var logEvent = logQueue.take()
				while (logEvent !== POISON_PILL) {
					for (appenderWithLevel in appenders) {
						val effectiveMinLevel = appenderWithLevel.minLogLevel ?: this.minLogLevel
						if (logEvent.level >= effectiveMinLevel) {
							appenderWithLevel.appender.append(logEvent.level, logEvent.message, logEvent.throwable)
						}
					}
					logEvent = logQueue.take()
				}
			} catch (e: InterruptedException) {
				Thread.currentThread().interrupt()
			}
		}
	}

	override fun log(level: LogLevel, message: String, throwable: Throwable?) {
		if (!logExecutor.isShutdown) {
			val event = LogEvent(level, message, throwable)
			logQueue.offer(event)
		}
	}

	override fun v(message: String, throwable: Throwable?) = log(LogLevel.VERBOSE, message, throwable)

	override fun s(message: String, throwable: Throwable?) = log(LogLevel.SUCCESS, message, throwable)

	override fun d(message: String, throwable: Throwable?) = log(LogLevel.DEBUG, message, throwable)

	override fun i(message: String, throwable: Throwable?) = log(LogLevel.INFO, message, throwable)

	override fun w(message: String, throwable: Throwable?) = log(LogLevel.WARNING, message, throwable)

	override fun e(message: String, throwable: Throwable?) = log(LogLevel.ERROR, message, throwable)

	fun setMinLogLevel(level: LogLevel) {
		minLogLevel = level
	}

	fun shutdown() {
		logQueue.offer(POISON_PILL)
		logExecutor.shutdown()
		try {
			if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				System.err.println("Logger shutdown timed out. Forcing shutdown...")
				logExecutor.shutdownNow()
			}
		} catch (e: InterruptedException) {
			logExecutor.shutdownNow()
			Thread.currentThread().interrupt()
		}
	}
}
