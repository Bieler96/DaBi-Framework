package de.bieler.dabilogger.core.appender

import de.bieler.dabilogger.core.LogLevel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class FileAppender(private val logFile: File) : Appender {
	private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

	init {
		if (!logFile.exists()) {
			logFile.createNewFile()
		}
	}

	override fun append(
		level: LogLevel,
		tag: String,
		message: String,
		throwable: Throwable?
	) {
		val timestamp = dateFormatter.format(Date())
		val logLine = "$timestamp [${level.name}] $tag: $message\n" +
				(throwable?.let { "${it.stackTraceToString()}\n" } ?: "")

		FileWriter(logFile, true).use { writer ->
			writer.append(logLine)
		}
	}
}