package utils.executePeriodically

import kotlinx.coroutines.delay

/**
 * Executes a given action periodically until a stop condition is met, a maximum number of attempts is reached, or an optional timeout occurs.
 *
 * @param intervalMillis The interval in milliseconds between each execution of the action. Default is 3000 milliseconds.
 * @param maxAttempts The maximum number of attempts to execute the action. Default is Int.MAX_VALUE.
 * @param timeout An optional timeout in milliseconds after which the execution will stop. Default is null.
 * @param stopCondition A lambda function that returns a Boolean indicating whether to stop the execution. Default is a function that always returns false.
 * @param action A suspend function representing the action to be executed periodically.
 * @param onResult A lambda function to handle the result of the action if it is not null. Default is an empty function.
 * @param onError A lambda function to handle any exceptions thrown by the action. Default is an empty function.
 */
suspend fun <T> executePeriodically(
	intervalMillis: Long = 3000,
	maxAttempts: Int = Int.MAX_VALUE,
	timeout: Long? = null,
	stopCondition: () -> Boolean = { false },
	action: suspend () -> T?,
	onResult: (T) -> Unit = {},
	onError: (Throwable) -> Unit = {}
) {
	var attempts = 0
	val startTime = System.currentTimeMillis()

	while (attempts++ < maxAttempts) {
		if (timeout != null && System.currentTimeMillis() - startTime >= timeout) break
		if (stopCondition()) break

		try {
			val result = action()

			if (result != null) {
				onResult(result)
			}
		} catch (e: Exception) {
			onError(e)
		}

		if (attempts < maxAttempts)
			delay(intervalMillis)
	}
}