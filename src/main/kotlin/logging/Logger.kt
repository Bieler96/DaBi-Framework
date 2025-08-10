package logging

import logging.appender.Appender
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class Logger(
	private val appenders: List<Appender>,
	@Volatile private var minLogLevel: LogLevel = LogLevel.INFO
) : LoggerBase {
	private val logQueue = LinkedBlockingQueue<LogEvent>()
	private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
		Thread(r, "dabi-logger-thread").apply {
			isDaemon = true
		}
	}

	companion object {
		private val POISON_PILL = LogEvent(LogLevel.INFO, "", "", null)
	}

	data class LogEvent(
		val level: LogLevel,
		val tag: String,
		val message: String,
		val throwable: Throwable?
	)

	init {
		logExecutor.submit {
			try {
				var logEvent = logQueue.take()
				while (logEvent !== POISON_PILL) {
					for (appender in appenders) {
						appender.append(logEvent.level, logEvent.tag, logEvent.message, logEvent.throwable)
					}
					logEvent = logQueue.take()
				}
			} catch (e: InterruptedException) {
				Thread.currentThread().interrupt()
			}
		}
	}

	override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
		if (level.ordinal >= minLogLevel.ordinal) {
			if (!logExecutor.isShutdown) {
				val event = LogEvent(level, tag, message, throwable)
				logQueue.offer(event)
			}
		}
	}

	override fun v(tag: String, message: String, throwable: Throwable?) = log(LogLevel.VERBOSE, tag, message, throwable)

	override fun s(tag: String, message: String, throwable: Throwable?) = log(LogLevel.SUCCESS, tag, message, throwable)

	override fun d(tag: String, message: String, throwable: Throwable?) = log(LogLevel.DEBUG, tag, message, throwable)

	override fun i(tag: String, message: String, throwable: Throwable?) = log(LogLevel.INFO, tag, message, throwable)

	override fun w(tag: String, message: String, throwable: Throwable?) = log(LogLevel.WARNING, tag, message, throwable)

	override fun e(tag: String, message: String, throwable: Throwable?) = log(LogLevel.ERROR, tag, message, throwable)

	fun setMinLogLevel(level: LogLevel) {
		minLogLevel = level
	}

	fun shutdown() {
		logExecutor.shutdown()
		logQueue.offer(POISON_PILL)

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