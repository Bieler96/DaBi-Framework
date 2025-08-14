package server.annotations

/**
 * Marks a class as a controller.
 * @param path The base path for all routes in this controller.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Controller(val path: String = "")