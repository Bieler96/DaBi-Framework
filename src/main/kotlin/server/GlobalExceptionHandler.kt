package server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import server.model.ErrorResponse

fun Application.installGlobalExceptionHandler() {
	install(StatusPages) {
		exception<Throwable> { call, cause ->
			println("Unhandled exception: ${cause}")
			val status = HttpStatusCode.InternalServerError
			call.respond(status, ErrorResponse(cause.message ?: "An unexpected error occurred", status.value))
		}

		exception<IllegalArgumentException> { call, cause ->
			val status = HttpStatusCode.BadRequest
			call.respond(status, ErrorResponse(cause.message, status.value))
		}
	}
}