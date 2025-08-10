package de.bieler.dabilogger.core.appender

import de.bieler.dabilogger.core.LogLevel

interface Appender {
	fun append(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}