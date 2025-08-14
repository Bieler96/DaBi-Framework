package di.annotations

/**
 * Annotation zur Kennzeichnung einer Klasse, die über DI bereitgestellt werden kann.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Injectable