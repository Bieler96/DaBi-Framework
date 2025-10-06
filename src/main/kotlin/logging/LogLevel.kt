package logging

data class LogLevel(val name: String, val severity: Int) : Comparable<LogLevel> {
    override fun compareTo(other: LogLevel): Int {
        return this.severity.compareTo(other.severity)
    }

    companion object {
        val VERBOSE = LogLevel("VERBOSE", 0)
        val DEBUG = LogLevel("DEBUG", 10)
        val INFO = LogLevel("INFO", 20)
        val SUCCESS = LogLevel("SUCCESS", 25)
        val WARNING = LogLevel("WARNING", 30)
        val ERROR = LogLevel("ERROR", 40)

        private val customLevels = mutableMapOf<String, LogLevel>()

        fun addCustomLevel(name: String, severity: Int): LogLevel {
            val upperCaseName = name.uppercase()
            if (listOf("VERBOSE", "DEBUG", "INFO", "SUCCESS", "WARNING", "ERROR").contains(upperCaseName)) {
                throw IllegalArgumentException("Cannot override default log levels.")
            }
            val newLevel = LogLevel(upperCaseName, severity)
            customLevels[upperCaseName] = newLevel
            return newLevel
        }

        fun levelFromName(name: String): LogLevel? {
            return when (name.uppercase()) {
                "VERBOSE" -> VERBOSE
                "DEBUG" -> DEBUG
                "INFO" -> INFO
                "SUCCESS" -> SUCCESS
                "WARNING" -> WARNING
                "ERROR" -> ERROR
                else -> customLevels[name.uppercase()]
            }
        }
    }
}
