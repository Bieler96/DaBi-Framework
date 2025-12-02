package server

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Controller(val path: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequestMapping(
    val path: String,
    val method: HttpMethod,
    val summary: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GetMapping(
    val path: String,
    val summary: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostMapping(
    val path: String,
    val summary: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PutMapping(
    val path: String,
    val summary: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PatchMapping(
    val path: String,
    val summary: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DeleteMapping(
    val path: String,
    val summary: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequestParam(
    val name: String = "",
    val required: Boolean = true,
    val defaultValue: String = "",
    val description: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequestBody(
    val description: String = "",
    val required: Boolean = true,
    val type: KClass<*> = Any::class
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ApiResponse(
    val statusCode: Int,
    val description: String,
    val content: Array<Content> = []
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiResponses(
    val value: Array<ApiResponse>
)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class PathParameter(
    val name: String,
    val description: String = "",
    val type: KClass<*> = String::class,
    val format: String = ""
)

annotation class Content(
    val mediaType: String = "application/json",
    val schema: KClass<*> = Unit::class,
    val example: String = ""
)
