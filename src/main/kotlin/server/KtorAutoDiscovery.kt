package server

import io.github.classgraph.ClassGraph
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import server.annotations.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

/**
 * Discovers and registers controllers in the specified base package.
 * @param basePackage The package to scan for controllers.
 */
fun Application.autoDiscoverRoutes(basePackage: String) {
	routing {
		val controllerClasses = ClassGraph()
			.enableAllInfo()
			.acceptPackages(basePackage)
			.scan()
			.getClassesWithAnnotation(Controller::class.java.name)
			.loadClasses()
			.map { it.kotlin }

		for (controllerClass in controllerClasses) {
			val controllerInstance = controllerClass.primaryConstructor?.call()
				?: throw IllegalStateException("Controller ${controllerClass.simpleName} must have a primary constructor.")

			val controllerPath = controllerClass.findAnnotation<Controller>()?.path ?: ""

			for (function in controllerClass.declaredFunctions) {
				val (httpMethod, path) = function.annotations.mapNotNull { annotation ->
					when (annotation) {
						is GetMapping -> HttpMethod.Get to annotation.path
						is PostMapping -> HttpMethod.Post to annotation.path
						is PutMapping -> HttpMethod.Put to annotation.path
						is DeleteMapping -> HttpMethod.Delete to annotation.path
						is PatchMapping -> HttpMethod.Patch to annotation.path
						else -> null
					}
				}.firstOrNull() ?: continue

				val fullPath = if (controllerPath.endsWith("/") || path.startsWith("/")) {
					"$controllerPath$path"
				} else {
					"$controllerPath/$path"
				}.replace(Regex("/+"), "/")

				route(fullPath, httpMethod) {
					handle {
						val callParameters = mutableMapOf<KParameter, Any?>()
						function.instanceParameter?.let { callParameters[it] = controllerInstance }

						for (param in function.valueParameters) {
							val value = when {
								param.hasAnnotation<Body>() -> {
									val type = param.type
									val classifier = type.classifier as? KClass<*>
									if (classifier != null) {
										call.receive(classifier)
									} else {
										throw IllegalStateException("Cannot receive body for type ${type.toString()}")
									}
								}

								param.hasAnnotation<PathParam>() -> {
									val paramName = param.findAnnotation<PathParam>()!!.name
									val rawValue = call.parameters[paramName]
									convertParameter(rawValue, param.type.classifier as KClass<*>)
								}

								param.hasAnnotation<QueryParam>() -> {
									val paramName = param.findAnnotation<QueryParam>()!!.name
									val rawValue = call.request.queryParameters[paramName]
									convertParameter(rawValue, param.type.classifier as KClass<*>)
								}

								param.type.isSubtypeOf(ApplicationCall::class.starProjectedType) -> call
								else -> null
							}
							callParameters[param] = value
						}

						val result = function.callSuspendBy(callParameters)
						if (result != Unit && result != null) {
							call.respond(result)
						}
					}
				}
			}
		}
	}
}

private fun convertParameter(value: String?, type: KClass<*>): Any? {
	if (value == null) return null
	return when (type) {
		String::class -> value
		Int::class -> value.toIntOrNull()
		Long::class -> value.toLongOrNull()
		Float::class -> value.toFloatOrNull()
		Double::class -> value.toDoubleOrNull()
		Boolean::class -> value.toBoolean()
		else -> throw IllegalArgumentException("Unsupported parameter type: ${type.simpleName}")
	}
}