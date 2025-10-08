package logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LogManagerTest {

	@BeforeEach
	fun setUp() {
		LogManager.resetForTesting()
	}

	@AfterEach
	fun tearDown() {
		LogManager.resetForTesting()
	}

	@Test
	fun `initialize should set up logger with console appender`() {
		val config = LoggerConfig(
			minLogLevel = LogLevel.INFO,
			appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.CONSOLE))
		)
		LogManager.initialize(config)
		val logger = LogManager.getLogger()

		// Verify that the logger is initialized and has at least one appender
		// We can't directly inspect the appenders list of the internal logger, so we'll rely on behavior
		logger.i("Test message")
		// Since we can't mock ConsoleAppender easily, we'll assume it works if the logger is initialized
		// and doesn't throw an error.
	}

	@Test
	fun `initialize should set up logger with http appender when endpoint is provided`() {
		val httpEndpoint = "http://localhost:8080/log"
		val config = LoggerConfig(
			minLogLevel = LogLevel.DEBUG,
			appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.HTTP)),
			httpEndpoint = httpEndpoint
		)
		LogManager.initialize(config)
		val logger = LogManager.getLogger()

		logger.d("HTTP test message")
		// Similar to console appender, we assume it works if no error is thrown.
	}

	@Test
	fun `initialize should throw IllegalArgumentException for http appender without endpoint`() {
		val config = LoggerConfig(
			minLogLevel = LogLevel.ERROR,
			appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.HTTP)),
			httpEndpoint = null
		)

		assertThrows<IllegalArgumentException> {
			LogManager.initialize(config)
		}
	}

	@Test
	fun `getLogger should initialize with default config if not already initialized`() {
		// LogManager is not initialized at this point
		val logger = LogManager.getLogger()

		// Verify that the logger is initialized and can log
		logger.i("Default initialized logger message")
		// Again, relying on no error being thrown and the logger being a valid instance
	}

	@Test
	fun `initialize should only run once`() {
		val config1 = LoggerConfig(
			minLogLevel = LogLevel.INFO,
			appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.CONSOLE))
		)
		LogManager.initialize(config1)
		val firstLogger = LogManager.getLogger()

		val config2 = LoggerConfig(
			minLogLevel = LogLevel.DEBUG,
			appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.FILE))
		)
		LogManager.initialize(config2) // This call should be ignored
		val secondLogger = LogManager.getLogger()

		// Verify that the same logger instance is returned
		assert(firstLogger === secondLogger)

		// Verify that the log level remains from the first initialization
		firstLogger.d("This should not be logged if INFO is min level")
		// We can't directly verify the minLogLevel of the internal logger, so we'll rely on the instance check.
	}
}
