# GEMINI.md

## Project Overview

This project is a modular and extensible logging framework written in Kotlin. It provides a simple and efficient way to log messages to various outputs, such as the console, files, and HTTP endpoints. The framework is designed to be easy to use and configure, with a focus on performance and flexibility.

### Main Technologies

*   **Kotlin:** The primary programming language used for the framework.
*   **JSON:** Used for structured logging in the `JsonFileAppender`.

### Architecture

The logging framework is built around a few core components:

*   **`LogManager`:** A singleton that manages the logger instance. It is responsible for initializing the logger with a given configuration and providing access to it.
*   **`Logger`:** The main class that handles log events. It uses a single-threaded executor to process log messages asynchronously, ensuring that logging operations do not block the main application thread.
*   **`LoggerConfig`:** A data class that holds the configuration for the logger, such as the minimum log level and the list of appenders to use.
*   **`Appender`:** An interface that defines the contract for all log appenders. The framework includes several built-in appenders:
    *   `ConsoleAppender`: For logging to the standard output.
    *   `FileAppender`: For writing logs to a text file.
    *   `JsonFileAppender`: For writing logs to a file in JSON format.
    *   `HttpAppender`: For sending logs to an HTTP endpoint.

## Building and Running

As this is a library project, there is no main entry point to run. To use the logger in your project, you would typically initialize it once with a `LoggerConfig` and then obtain the logger instance from the `LogManager`.

### Example Usage

```kotlin
import logging.LogManager
import logging.LoggerConfig
import logging.LogLevel

fun main() {
    // Configure the logger
    val config = LoggerConfig(
        minLogLevel = LogLevel.DEBUG,
        appenders = listOf(LoggerConfig.AppenderType.CONSOLE, LoggerConfig.AppenderType.FILE)
    )

    // Initialize the logger
    LogManager.initialize(config)

    // Get the logger instance
    val logger = LogManager.getLogger()

    // Log messages
    logger.i("Main", "This is an info message")
    logger.d("Main", "This is a debug message")
    logger.e("Main", "This is an error message", Exception("Something went wrong"))
}
```

## Development Conventions

*   **Coding Style:** The codebase follows standard Kotlin coding conventions.
*   **Immutability:** Data classes like `LoggerConfig` and `LogEvent` are used to promote immutability.
*   **Asynchronous Logging:** All logging operations are performed asynchronously to avoid blocking the main thread.
*   **Extensibility:** The `Appender` interface allows for easy creation of custom appenders to support different logging targets.
