package logging.dsl

import logging.AppenderConfigEntry
import logging.LogLevel
import logging.LoggerConfig
import logging.LogManager

@DslMarker
annotation class LoggingDslMarker

@LoggingDslMarker
class LoggingDsl {
    var minLogLevel: LogLevel = LogLevel.INFO
    private var appenderConfigs = listOf<AppenderConfigEntry>()
    private var httpEndpoint: String? = null
    private var logFile: String = "application.log"
    private var jsonLogFile: String = "application.json"

    fun customLevel(name: String, severity: Int) {
        LogLevel.addCustomLevel(name, severity)
    }

    fun appenders(block: AppendersDsl.() -> Unit) {
        val appendersDsl = AppendersDsl().apply(block)
        appenderConfigs = appendersDsl.appenderConfigs
        httpEndpoint = appendersDsl.httpEndpoint
        logFile = appendersDsl.logFile
        jsonLogFile = appendersDsl.jsonLogFile
    }

    internal fun build(): LoggerConfig {
        return LoggerConfig(
            minLogLevel = minLogLevel,
            appenders = appenderConfigs,
            httpEndpoint = httpEndpoint,
            logFile = logFile,
            jsonLogFile = jsonLogFile
        )
    }
}

@LoggingDslMarker
class AppendersDsl {
    internal val appenderConfigs = mutableListOf<AppenderConfigEntry>()
    internal var httpEndpoint: String? = null
    internal var logFile: String = "application.log"
    internal var jsonLogFile: String = "application.json"

    fun console(minLogLevel: LogLevel? = null) {
        appenderConfigs.add(AppenderConfigEntry(LoggerConfig.AppenderType.CONSOLE, minLogLevel))
    }

    fun file(fileName: String = "application.log", minLogLevel: LogLevel? = null) {
        appenderConfigs.add(AppenderConfigEntry(LoggerConfig.AppenderType.FILE, minLogLevel))
        logFile = fileName
    }

    fun json(fileName: String = "application.json", minLogLevel: LogLevel? = null) {
        appenderConfigs.add(AppenderConfigEntry(LoggerConfig.AppenderType.JSON_FILE, minLogLevel))
        jsonLogFile = fileName
    }

    fun http(endpoint: String, minLogLevel: LogLevel? = null) {
        appenderConfigs.add(AppenderConfigEntry(LoggerConfig.AppenderType.HTTP, minLogLevel))
        httpEndpoint = endpoint
    }
}

/**
 * Configures the logging framework using a DSL.
 *
 * Example:
 * ```kotlin
 * logging {
 *     minLogLevel = LogLevel.DEBUG
 *     appenders {
 *         console()
 *         file("my-app.log")
 *         json("my-app.json")
 *         http("http://localhost:8080/logs")
 *     }
 * }
 * ```
 */
fun logging(block: LoggingDsl.() -> Unit) {
    val config = LoggingDsl().apply(block).build()
    LogManager.initialize(config)
}
