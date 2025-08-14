package server.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val message: String?, val httpStatusCode: Int)