package logging

import logging.appender.Appender
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions

class LoggerTest {
    @Test
    fun `log method should append message when log level is sufficient`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(mockAppender))
        logger.setMinLogLevel(LogLevel.INFO)
        logger.i("TEST_TAG", "This is an info message")

        verify(mockAppender, timeout(1000)).append(LogLevel.INFO, "TEST_TAG", "This is an info message", null)
        logger.shutdown()
    }

    @Test
    fun `log method should not append message when log level is too low`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(mockAppender))
        logger.setMinLogLevel(LogLevel.WARNING)
        logger.i("TEST_TAG", "This is an info message") // INFO < WARNING

        verifyNoInteractions(mockAppender)
        logger.shutdown()
    }

    @Test
    fun `shutdown method should terminate the logger thread`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(mockAppender))
        logger.i("TEST_TAG", "Message before shutdown")
        logger.shutdown()

        // Verify that the message was processed before shutdown
        verify(mockAppender, timeout(1000)).append(LogLevel.INFO, "TEST_TAG", "Message before shutdown", null)

        // Attempt to log after shutdown, should not be processed
        logger.e("TEST_TAG", "Message after shutdown")
        Thread.sleep(200) // Give a small delay to ensure no unexpected appends

        verifyNoMoreInteractions(mockAppender)
    }

    @Test
    fun `log method should handle throwable correctly`() {
        val mockAppender: Appender = mock(Appender::class.java)
        val logger = Logger(listOf(mockAppender))
        logger.setMinLogLevel(LogLevel.ERROR)
        val exception = RuntimeException("Test Exception")
        logger.e("ERROR_TAG", "An error occurred", exception)

        verify(mockAppender, timeout(1000)).append(LogLevel.ERROR, "ERROR_TAG", "An error occurred", exception)
        logger.shutdown()
    }
}