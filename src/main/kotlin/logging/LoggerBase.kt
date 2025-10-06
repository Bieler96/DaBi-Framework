package logging

interface LoggerBase {
	fun log(level: LogLevel, message: String, throwable: Throwable? = null)
	fun v(message: String, throwable: Throwable? = null)
	fun s(message: String, throwable: Throwable? = null)
	fun d(message: String, throwable: Throwable? = null)
	fun i(message: String, throwable: Throwable? = null)
	fun w(message: String, throwable: Throwable? = null)
	fun e(message: String, throwable: Throwable? = null)
}