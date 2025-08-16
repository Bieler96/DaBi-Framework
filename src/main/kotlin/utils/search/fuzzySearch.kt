package utils.search

fun <T> fuzzySearch(query: String, candidates: List<T>, selector: (T) -> String): List<T> {
	if (query.isBlank()) return candidates

	val maxDistance = dynamicMaxDistance(query)
	val lowercaseQuery = query.lowercase()

	return candidates
		.map { item ->
			val text = selector(item).lowercase()
			val isSubstring = text.contains(lowercaseQuery)
			val distance = if (isSubstring) 0 else levenshtein(lowercaseQuery, text)
			item to distance
		}
		.filter { it.second <= maxDistance }
		.sortedBy { it.second }
		.map { it.first }
}

private fun dynamicMaxDistance(query: String): Int {
	return when {
		query.length <= 3 -> 1
		query.length <= 6 -> 2
		else -> query.length / 3
	}
}