package utils.exception

import io.ktor.http.*

class UnexpectedStatusException(status: HttpStatusCode, body: String) :
	Exception("Unexpected status ${status.value}: $body")