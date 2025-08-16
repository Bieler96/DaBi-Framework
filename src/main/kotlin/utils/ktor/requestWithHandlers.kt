package utils.ktor

import utils.exception.UnexpectedStatusException
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

suspend inline fun <reified T> HttpClient.requestWithHandlers(
	method: HttpMethod,
	url: String,
	vararg expectedHandlers: Pair<HttpStatusCode, suspend (HttpResponse) -> T>
): T {
	val expectedMap = expectedHandlers.toMap()

	val response = request {
		this.url(url)
		this.method = method
	}

	val handler = expectedMap[response.status]
	return if (handler != null) {
		handler(response)
	} else {
		val errorText = response.bodyAsText()
		throw UnexpectedStatusException(response.status, errorText)
	}
}