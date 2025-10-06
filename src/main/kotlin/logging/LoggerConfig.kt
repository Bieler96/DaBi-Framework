package logging

data class LoggerConfig(
	val minLogLevel: LogLevel = LogLevel.INFO,
	val appenders: List<AppenderType> = listOf(AppenderType.CONSOLE),
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