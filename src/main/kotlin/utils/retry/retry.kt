package utils.retry

import kotlinx.coroutines.delay

suspend fun <T> retry(
	times: Int,
	initialDelay: Long = 1000,
	maxDelay: Long = 10000,
	factor: Double = 2.0,
	block: suspend () -> T,
	onError: (Throwable) -> Unit = {}
) {
	var currentDelay = initialDelay
	repeat(times - 1) {
		try {
			return block() as Unit
		} catch (e: Exception) {
			onError(e)
		}
		delay(currentDelay)
		currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
	}
	return block() as Unit
}