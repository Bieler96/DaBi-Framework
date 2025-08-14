package server.annotations

/**
 * Annotation for mapping HTTP GET requests onto specific handler methods.
 * @param path The path for this mapping.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GetMapping(val path: String = "")

/**
 * Annotation for mapping HTTP POST requests onto specific handler methods.
 * @param path The path for this mapping.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostMapping(val path: String = "")

/**
 * Annotation for mapping HTTP PUT requests onto specific handler methods.
 * @param path The path for this mapping.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PutMapping(val path: String = "")

/**
 * Annotation for mapping HTTP DELETE requests onto specific handler methods.
 * @param path The path for this mapping.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DeleteMapping(val path: String = "")

/**
 * Annotation for mapping HTTP PATCH requests onto specific handler methods.
 * @param path The path for this mapping.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PatchMapping(val path: String = "")