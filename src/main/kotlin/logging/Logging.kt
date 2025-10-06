package logging

/**
 * Global logger instance.
 *
 * It is initialized with a default configuration (ConsoleAppender, LogLevel.DEBUG)
 * on its first use if no other configuration has been provided.
 *
 * You can customize the logger by calling `logging { ... }` at the start of your application.
 */
val logger: Logger by lazy {
    LogManager.getLogger()
}
