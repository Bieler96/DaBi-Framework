package utils.retry

import kotlinx.coroutines.delay

/**
 * Retries the given suspending block of code with an exponential backoff strategy.
 *
 * @param times The number of times to retry the block.
 * @param initialDelay The initial delay before the first retry in milliseconds. Default is 1000 ms.
 * @param maxDelay The maximum delay between retries in milliseconds. Default is 10000 ms.
 * @param factor The factor by which the delay is multiplied after each retry. Default is 2.0.
 * @param block The suspending block of code to be retried.
 * @param onError A function to be called with the exception if the block fails. Default is an empty function.
 * @return The result of the block if it succeeds within the given number of retries.
 * @throws Exception if the block fails after the given number of retries.
 */
suspend fun <T> retryWithBackoff(
    times: Int,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T,
    onError: (Throwable) -> Unit = {}
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            onError(e)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}