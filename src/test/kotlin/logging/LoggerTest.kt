package logging

import logging.appender.Appender
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import java.io.File
import java.io.IOException

class LoggerTest {
    @Test
    fun `log method should append message when log level is sufficient`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(AppenderWithLevel(mockAppender, null)))
        logger.setMinLogLevel(LogLevel.INFO)
        logger.i("This is an info message")

        verify(mockAppender, timeout(1000)).append(LogLevel.INFO, "This is an info message", null)
        logger.shutdown()
    }

    @Test
    fun `log method should not append message when log level is too low`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(AppenderWithLevel(mockAppender, null)))
        logger.setMinLogLevel(LogLevel.WARNING)
        logger.i("This is an info message") // INFO < WARNING

        verifyNoInteractions(mockAppender)
        logger.shutdown()
    }

    @Test
    fun `shutdown method should terminate the logger thread`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(AppenderWithLevel(mockAppender, null)))
        logger.i("Message before shutdown")
        logger.shutdown()

        // Verify that the message was processed before shutdown
        verify(mockAppender, timeout(1000)).append(LogLevel.INFO, "Message before shutdown", null)

        // Attempt to log after shutdown, should not be processed
        logger.e("Message after shutdown")
        Thread.sleep(200) // Give a small delay to ensure no unexpected appends

        verifyNoMoreInteractions(mockAppender)
    }

    @Test
    fun `log method should handle throwable correctly`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(AppenderWithLevel(mockAppender, null)))
        logger.setMinLogLevel(LogLevel.ERROR)
        val exception = RuntimeException("Test Exception")
        logger.e("An error occurred", exception)

        verify(mockAppender, timeout(1000)).append(LogLevel.ERROR, "An error occurred", exception)
        logger.shutdown()
    }

    @Test
    fun `xml appender should write correctly formatted log to file`() {
        val testLogFile = "test_log.xml"
        val file = File(testLogFile)
        try {
            // 1. Configure and initialize LogManager
            val config = LoggerConfig(
                minLogLevel = LogLevel.DEBUG,
                appenders = listOf(AppenderConfigEntry(LoggerConfig.AppenderType.XML_FILE)),
                xmlLogFile = testLogFile
            )
            LogManager.initialize(config)

            // 2. Get logger and log messages
            val logger = LogManager.getLogger()
            val exception = IOException("File not found")
            logger.i("This is an info message.")
            logger.e("This is an error message.", exception)

            // 3. Shutdown logger to ensure all messages are written
            LogManager.logger.shutdown()

            // 4. Read file and verify content
            val logContent = file.readText()

            assertTrue(logContent.contains("<level>INFO</level>"))
            assertTrue(logContent.contains("<message>This is an info message.</message>"))
            assertTrue(logContent.contains("<level>ERROR</level>"))
            assertTrue(logContent.contains("<message>This is an error message.</message>"))
            assertTrue(logContent.contains("<exception>java.io.IOException: File not found"))

        } finally {
            // 5. Clean up
            if (file.exists()) {
                file.delete()
            }
            LogManager.resetForTesting() // Reset for other tests
        }
    }
}