package utils.cache

/**
 * A simple cache implementation that stores key-value pairs and invalidates them after a specified time.
 *
 * @param expiryMillis The time in milliseconds after which the cache entries expire.
 */
class Cache<K, V>(private val expiryMillis: Long) {
    private val cache = mutableMapOf<K, Pair<V, Long>>()

    /**
     * Retrieves a value from the cache or loads it using the provided loader function if not present or expired.
     *
     * @param key The key to look up in the cache.
     * @param loader A function to load the value if it is not present or expired.
     * @return The cached or newly loaded value.
     */
    fun get(key: K, loader: () -> V): V {
        val currentTime = System.currentTimeMillis()
        val cachedValue = cache[key]

        return if (cachedValue != null && currentTime - cachedValue.second < expiryMillis) {
            cachedValue.first
        } else {
            val newValue = loader()
            cache[key] = newValue to currentTime
            newValue
        }
    }
}