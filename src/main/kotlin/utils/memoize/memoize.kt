package utils.memoize

fun <T, R> memoize(function: (T) -> R): (T) -> R {
    val cache = mutableMapOf<T, R>()

    return { input: T ->
        cache.getOrPut(input) { function(input) }
    }
}