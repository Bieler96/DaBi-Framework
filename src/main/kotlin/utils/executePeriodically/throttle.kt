package utils.executePeriodically

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Creates a throttled version of the given function that ensures the function
 * is not called more than once within the specified wait time.
 *
 * @param waitMillis The minimum time interval between successive calls in milliseconds.
 * @param scope The CoroutineScope in which the function will be executed.
 * @param function The function to be throttled.
 * @return A throttled version of the given function.
 */
fun <T> throttle(waitMillis: Long, scope: CoroutineScope, function: (T) -> Unit): (T) -> Unit {
	val lastExecutionTime = AtomicLong(0)
	return { param: T ->
		val currentTime = System.currentTimeMillis()
		if (currentTime - lastExecutionTime.get() >= waitMillis) {
			lastExecutionTime.set(currentTime)
			scope.launch {
				function(param)
			}
		}
	}
}