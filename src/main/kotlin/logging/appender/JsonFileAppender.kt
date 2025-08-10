package core.appender

import de.bieler.dabilogger.core.LogLevel
import de.bieler.dabilogger.core.appender.Appender
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JsonFileAppender(private val filePath: String) : Appender {
	private val file = File(filePath)
	private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

	init {
		if (!file.exists()) {
			FileWriter(file).use { it.write("[]") }
		}
	}

	override fun append(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
		val jsonArray = if (file.length() > 0) {
			JSONArray(file.readText())
		} else {
			JSONArray()
		}

		val logEntry = JSONObject()
		logEntry.put("timestamp", LocalDateTime.now().format(dateFormatter))
		logEntry.put("level", level.name)
		logEntry.put("tag", tag)
		logEntry.put("message", message)

		if (throwable != null) {
			logEntry.put("exception", throwable.toString())
		}

		jsonArray.put(logEntry)

		FileWriter(file).use { writer ->
			writer.write(jsonArray.toString(2))
		}
	}
}

