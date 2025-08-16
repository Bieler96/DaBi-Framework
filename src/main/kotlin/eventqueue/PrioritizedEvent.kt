package eventqueue

import kotlinx.coroutines.CompletableDeferred

data class PrioritizedEvent<T>(
	val priority: Int,
	val event: T,
	val timeoutMillis: Long,
	var retriesLeft: Int,
	val result: CompletableDeferred<Result<T>>,
	val timestamp: Long = System.currentTimeMillis()
) : Comparable<PrioritizedEvent<T>> {
	override fun compareTo(other: PrioritizedEvent<T>): Int {
		return this.priority.compareTo(other.priority)
	}
}