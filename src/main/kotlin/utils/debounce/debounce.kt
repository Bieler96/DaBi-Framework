package utils.debounce

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Creates a debounced version of the given function that delays the execution
 * until after the specified wait time has elapsed since the last time it was invoked.
 *
 * @param waitMillis The time to wait in milliseconds before executing the function.
 * @param scope The CoroutineScope in which the function will be executed.
 * @param function The function to be debounced.
 * @return A debounced version of the given function.
 */
fun <T> debounce(waitMillis: Long, scope: CoroutineScope, function: (T) -> Unit): (T) -> Unit {
	var debounceJob: Job? = null
	return { param: T ->
		debounceJob?.cancel()
		debounceJob = scope.launch {
			delay(waitMillis)
			function(param)
		}
	}
}