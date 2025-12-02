package server

import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement


// Helper data class for route information
private data class RouteInfo(
    val httpMethod: HttpMethod,
    val path: String,
    val summary: String,
    val description: String
)

object OpenApiGenerator {

    fun generateOpenApiSpec(
        packages: Array<String>,
        info: Info
    ): OpenAPI {
        val openApi = OpenAPI(info = info)
        val classGraph = ClassGraph()
            .enableAllInfo()
            .acceptPackages(*packages)
            .scan()

        classGraph.use { scanResult ->
            for (classInfo in scanResult.getClassesWithAnnotation(Controller::class.java)) {
                val kClass = Class.forName(classInfo.name).kotlin
                val controller = kClass.findAnnotation<Controller>() ?: continue
                val controllerPath = controller.path

                for (function in kClass.declaredFunctions) {
                    function.annotations.forEach { annotation ->
                        val routeInfo = when (annotation) {
                            is GetMapping -> RouteInfo(HttpMethod.GET, annotation.path, annotation.summary, annotation.description)
                            is PostMapping -> RouteInfo(HttpMethod.POST, annotation.path, annotation.summary, annotation.description)
                            is PutMapping -> RouteInfo(HttpMethod.PUT, annotation.path, annotation.summary, annotation.description)
                            is PatchMapping -> RouteInfo(HttpMethod.PATCH, annotation.path, annotation.summary, annotation.description)
                            is DeleteMapping -> RouteInfo(HttpMethod.DELETE, annotation.path, annotation.summary, annotation.description)
                            else -> null
                        } ?: return@forEach

                        val (httpMethod, path, summary, description) = routeInfo

                        val fullPath = (controllerPath + path).replace(Regex("//+"), "/")
                        val operation = createOperation(function, summary, description)

                        val pathItem = openApi.paths.getOrPut(fullPath) { PathItem() }
                        when (httpMethod) {
                            HttpMethod.GET -> pathItem.get = operation
                            HttpMethod.POST -> pathItem.post = operation
                            HttpMethod.PUT -> pathItem.put = operation
                            HttpMethod.PATCH -> pathItem.patch = operation
                            HttpMethod.DELETE -> pathItem.delete = operation
                        }
                    }
                }
            }
        }
        return openApi
    }

    private fun createOperation(function: KFunction<*>, summary: String, description: String): Operation {
        val operation = Operation(summary = summary, description = if (description.isNotBlank()) description else null)

        // Parameters
        operation.parameters.addAll(function.findAnnotations<PathParameter>().map {
            Parameter(name = it.name, `in` = "path", required = true, description = it.description, schema = toSchema(it.type, it.format))
        })
        function.parameters.forEach { param ->
            param.findAnnotation<RequestParam>()?.let {
                operation.parameters.add(
                    Parameter(
                        name = it.name.ifBlank { param.name ?: "" }, // Fallback for param.name if null
                        `in` = "query",
                        required = it.required,
                        description = it.description,
                        schema = toSchema(param.type.classifier as KClass<*>)
                    )
                )
            }
        }

        // Request Body
        function.parameters.firstNotNullOfOrNull { it.findAnnotation<RequestBody>() }?.let { requestBody ->
            val kclass = function.parameters.first { it.findAnnotation<RequestBody>() != null }.type.classifier as KClass<*>
            operation.requestBody = RequestBodyObject(
                description = requestBody.description,
                content = mapOf("application/json" to MediaTypeObject(schema = toSchema(kclass))),
                required = requestBody.required
            )
        }

        // Responses
        function.findAnnotations<ApiResponse>().forEach { apiResponse ->
            val content = apiResponse.content.associate {
                it.mediaType to MediaTypeObject(
                    schema = if (it.schema != Unit::class) toSchema(it.schema) else null,
                    example = try {
                        if (it.example.isNotBlank()) Json.parseToJsonElement(it.example) else null
                    } catch (e: Exception) {
                        // Log the error or handle it as appropriate, e.g., return JsonNull
                        println("Warning: Could not parse example JSON for status code ${apiResponse.statusCode}: ${it.example}. Error: ${e.message}")
                        JsonNull
                    }
                )
            }
            operation.responses[apiResponse.statusCode.toString()] = ResponseObject(
                description = apiResponse.description,
                content = content.ifEmpty { null }
            )
        }

        return operation
    }

    private fun toSchema(kClass: KClass<*>, format: String = ""): Schema {
        return when (kClass) {
            Int::class -> Schema(type = "integer", format = "int32")
            Long::class -> Schema(type = "integer", format = "int64")
            Double::class -> Schema(type = "number", format = "double")
            Float::class -> Schema(type = "number", format = "float")
            String::class -> Schema(type = "string")
            Boolean::class -> Schema(type = "boolean")
            else -> Schema(type = "object", properties = getProperties(kClass))
        }
    }

    private fun getProperties(kClass: KClass<*>): Map<String, Schema> {
        return kClass.members
            .filter { it.parameters.size == 1} // constructor properties
            .associate { prop ->
                val propType = prop.returnType.classifier as KClass<*>
                prop.name to toSchema(propType)
            }
    }
}