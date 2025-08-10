package logging

enum class LogLevel {
	VERBOSE,
	DEBUG,
	INFO,
	WARNING,
	ERROR,
	SUCCESS
}

interface LoggerBase {
	fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
	fun v(tag: String, message: String, throwable: Throwable? = null)
	fun s(tag: String, message: String, throwable: Throwable? = null)
	fun d(tag: String, message: String, throwable: Throwable? = null)
	fun i(tag: String, message: String, throwable: Throwable? = null)
	fun w(tag: String, message: String, throwable: Throwable? = null)
	fun e(tag: String, message: String, throwable: Throwable? = null)
}