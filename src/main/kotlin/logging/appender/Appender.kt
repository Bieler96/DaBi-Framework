package logging.appender

import logging.LogLevel

interface Appender {
	fun append(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}