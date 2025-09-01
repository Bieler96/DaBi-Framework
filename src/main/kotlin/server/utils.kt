package server

import auth.User
import auth.auto_config.AuthenticatedUser
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import server.RequestBody
import server.RequestParam
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation

// Eine einfache Konvertierungsfunktion für Basis-Typen
fun convertValue(value: String, targetType: KClass<*>): Any = when (targetType) {
	Int::class -> value.toInt()
	Long::class -> value.toLong()
	Boolean::class -> value.toBoolean()
	Double::class -> value.toDouble()
	else -> value
}

suspend fun ApplicationCall.invokeControllerMethod(function: KFunction<*>, controllerInstance: Any) {
	val args = mutableMapOf<KParameter, Any?>()

	for (param in function.parameters) {
		when {
			// Der Instanzparameter (this) des Controllers
			param.kind == KParameter.Kind.INSTANCE -> args[param] = controllerInstance

			// Falls der Parameter vom Typ ApplicationCall ist
			param.type.classifier == ApplicationCall::class -> args[param] = this

			param.findAnnotation<AuthenticatedUser>() != null -> {
				if (param.type.classifier == User::class) {
					val user = principal<User>()
						?: throw IllegalStateException("No authenticated user found. Is the route protected by authenticate?")
					args[param] = user
				} else {
					throw IllegalStateException("@AuthenticatedUser annotation can only be used on parameters of type User.")
				}
			}

			// Parameter mit @RequestParam Annotation
			param.findAnnotation<RequestParam>() != null -> {
				val annotation = param.findAnnotation<RequestParam>()!!
				// Verwende den expliziten Namen oder den Parameternamen
				val paramName = if (annotation.name.isNotEmpty()) annotation.name else param.name
				val rawValue = request.queryParameters[paramName.toString()]
				val value = if (rawValue == null) {
					if (annotation.required) {
						throw IllegalArgumentException("Fehlender erforderlicher Query-Parameter: $paramName")
					} else {
						annotation.defaultValue
					}
				} else {
					// Konvertierung in den Zieltyp (z. B. Int, Boolean, etc.)
					convertValue(rawValue, param.type.classifier as KClass<*>)
				}
				args[param] = value
			}

			// Parameter mit @RequestBody Annotation
			param.findAnnotation<RequestBody>() != null -> {
				val typeInfo = TypeInfo(param.type.classifier as KClass<*>, param.type.platformType, param.type)
				val body = receive<Any>(typeInfo)
				args[param] = body
			}

			// Weitere Fälle können hier ergänzt werden (z. B. Path-Parameter)
		}
	}

	// Aufruf der (suspend) Funktion mit den extrahierten Parametern
	val result = function.callSuspendBy(args)
	result?.let { respond(it) }
}
