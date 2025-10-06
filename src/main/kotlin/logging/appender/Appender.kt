package logging.appender

import logging.LogLevel

interface Appender {
	fun append(level: LogLevel, message: String, throwable: Throwable? = null)
}