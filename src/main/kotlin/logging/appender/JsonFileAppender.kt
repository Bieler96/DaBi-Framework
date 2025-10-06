package logging.appender

import logging.LogLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.Instant

class JsonFileAppender(private val filePath: String) : Appender {
	private val file = File(filePath)

	init {
		if (!file.exists()) {
			FileWriter(file).use { it.write("[]") }
		}
	}

	override fun append(level: LogLevel, message: String, throwable: Throwable?) {
		val jsonArray = if (file.length() > 0) {
			JSONArray(file.readText())
		} else {
			JSONArray()
		}

		val logEntry = JSONObject()
		logEntry.put("timestamp", Instant.now().toString())
		logEntry.put("level", level.name)
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
