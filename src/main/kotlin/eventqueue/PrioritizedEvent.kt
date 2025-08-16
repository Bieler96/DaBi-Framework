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
	/**
	 * Compares this event with another for priority ordering.
	 * Higher priority values are considered "lesser" to ensure they are at the head of the min-heap PriorityQueue.
	 */
	override fun compareTo(other: PrioritizedEvent<T>): Int {
		return other.priority.compareTo(this.priority)
	}
}