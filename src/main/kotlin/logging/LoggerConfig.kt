package logging

data class AppenderConfigEntry(
	val type: LoggerConfig.AppenderType,
	val minLogLevel: LogLevel? = null
)

data class LoggerConfig(
	val minLogLevel: LogLevel = LogLevel.INFO,
	val appenders: List<AppenderConfigEntry> = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.CONSOLE)),
	val httpEndpoint: String? = null,
	val logFile: String = "application.log",
	val jsonLogFile: String = "application.json"
) {
	enum class AppenderType {
		CONSOLE,
		FILE,
		JSON_FILE,
		HTTP,
	}
}