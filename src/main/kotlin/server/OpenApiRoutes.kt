package server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.installOpenApi(
    info: Info,
    controllerPackages: Array<String>
) {
    val openApi = OpenApiGenerator.generateOpenApiSpec(controllerPackages, info)
    val openApiJson = Json { prettyPrint = true }.encodeToString(openApi)

    routing {
        get("/openapi.json") {
            call.respondText(openApiJson, ContentType.Application.Json)
        }

        get("/swagger") {
            call.respondText(getSwaggerUIHTML("/openapi.json"), ContentType.Text.Html)
        }
    }
}