package de.bieler.dabilogger.core

data class LoggerConfig(
	val minLogLevel: LogLevel = LogLevel.INFO,
	val appenders: List<AppenderType> = listOf(AppenderType.CONSOLE),
	val httpEndpoint: String? = null,
) {
	enum class AppenderType {
		CONSOLE,
		FILE,
		JSON_FILE,
		HTTP,
	}
}