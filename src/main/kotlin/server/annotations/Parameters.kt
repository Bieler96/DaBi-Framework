package server.annotations

/**
 * Binds a method parameter to a path parameter.
 * @param name The name of the path parameter.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class PathParam(val name: String)

/**
 * Binds a method parameter to a query parameter.
 * @param name The name of the query parameter.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class QueryParam(val name: String)

/**
 * Binds a method parameter to the request body.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Body