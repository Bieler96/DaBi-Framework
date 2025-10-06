package logging.appender

import logging.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsoleAppender : Appender {
	private companion object {
		const val RESET = "\u001B[0m"
		const val RED = "\u001B[31m"
		const val GREEN = "\u001B[32m"
		const val YELLOW = "\u001B[33m"
		const val BLUE = "\u001B[34m"
		const val PURPLE = "\u001B[35m"
		const val CYAN = "\u001B[36m"

		const val BG_RED = "\u001B[41m"
		const val BG_GREEN = "\u001B[42m"
		const val BG_YELLOW = "\u001B[43m"
		const val BG_BLUE = "\u001B[44m"
		const val BG_PURPLE = "\u001B[45m"
		const val BG_CYAN = "\u001B[46m"
	}

	private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

	override fun append(
		level: LogLevel,
		message: String,
		throwable: Throwable?
	) {
		val coloredLevel = when (level) {
			LogLevel.ERROR -> "$RED${level.name}$RESET"
			LogLevel.WARNING -> "$YELLOW${level.name}$RESET"
			LogLevel.INFO -> "$BLUE${level.name}$RESET"
			LogLevel.DEBUG -> "$PURPLE${level.name}$RESET"
			LogLevel.SUCCESS -> "$GREEN${level.name}$RESET"
			LogLevel.VERBOSE -> "$CYAN${level.name}$RESET"
			else -> level.name
		}
		val timestamp = dateFormatter.format(Date())

		val logMessage = "[$coloredLevel]\t$timestamp\t$message"

		when (level) {
//			LogLevel.ERROR -> System.err.println(logMessage)
			else -> println(logMessage)
		}
		throwable?.printStackTrace(System.err)
	}
}