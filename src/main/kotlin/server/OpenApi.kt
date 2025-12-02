package server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OpenAPI(
    var openapi: String,
    var info: Info,
    var paths: MutableMap<String, PathItem> = mutableMapOf()
)

@Serializable
data class Info(
    val title: String,
    val version: String,
    val description: String? = null
)

@Serializable
data class PathItem(
    var get: Operation? = null,
    var post: Operation? = null,
    var put: Operation? = null,
    var delete: Operation? = null,
    var patch: Operation? = null
)

@Serializable
data class Operation(
    var summary: String,
    var description: String? = null,
    var parameters: MutableList<Parameter> = mutableListOf(),
    var requestBody: RequestBodyObject? = null,
    var responses: MutableMap<String, ResponseObject> = mutableMapOf()
)

@Serializable
data class Parameter(
    val name: String,
    val `in`: String, // "query", "header", "path", "cookie"
    val required: Boolean,
    val description: String? = null,
    val schema: Schema? = null
)

@Serializable
data class RequestBodyObject(
    val description: String? = null,
    var content: Map<String, MediaTypeObject>,
    val required: Boolean = true
)

@Serializable
data class ResponseObject(
    val description: String,
    var content: Map<String, MediaTypeObject>? = null
)

@Serializable
data class MediaTypeObject(
    val schema: Schema? = null,
    val example: JsonElement? = null,
)

@Serializable
data class Schema(
    val type: String,
    val format: String? = null,
    val properties: Map<String, Schema>? = null,
    val items: Schema? = null,
    val example: JsonElement? = null,
    val description: String? = null,
    val required: List<String>? = null,
)