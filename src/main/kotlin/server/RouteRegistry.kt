package server

import auth.AuthController
import auth.auto_config.Authenticated
import di.DI
import io.github.classgraph.ClassGraph
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import server.Controller
import server.DeleteMapping
import server.GetMapping
import server.PatchMapping
import server.PostMapping
import server.PutMapping
import server.RequestMapping
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

fun Application.registerAnnotatedRoutes(
	vararg controllersPackages: String = arrayOf(
		"de.bieler.controller",
		"auth"
	)
) {
	routing {
		val scanResult = ClassGraph()
			.enableAllInfo()
			.acceptPackages(*controllersPackages)
			.scan()

		val controllerClasses = scanResult.allClasses

		for (classInfo in controllerClasses) {
			val kClass = Class.forName(classInfo.name).kotlin

			if (!kClass.annotations.any { it is Controller }) continue

			val controllerInstance = when (kClass) {
				AuthController::class -> AuthController(DI.get())
				else -> kClass.createInstance()
			}

			val controller = kClass.findAnnotation<Controller>()!!

			for (function in kClass.declaredFunctions) {
				val fullPath = "${controller.path}${function.findAnnotation<PostMapping>()?.path ?: ""}"

				function.annotations.forEach { annotation ->
					when (annotation) {
						is GetMapping -> registerRoute(
							this,
							HttpMethod.GET,
							"${controller.path}${annotation.path}",
							function,
							controllerInstance
						)

						is PostMapping -> registerRoute(
							this,
							HttpMethod.POST,
							"${controller.path}${annotation.path}",
							function,
							controllerInstance
						)

						is PutMapping -> registerRoute(
							this,
							HttpMethod.PUT,
							"${controller.path}${annotation.path}",
							function,
							controllerInstance
						)

						is PatchMapping -> registerRoute(
							this,
							HttpMethod.PATCH,
							"${controller.path}${annotation.path}",
							function,
							controllerInstance
						)

						is DeleteMapping -> registerRoute(
							this,
							HttpMethod.DELETE,
							"${controller.path}${annotation.path}",
							function,
							controllerInstance
						)

						is RequestMapping -> registerRoute(
							this,
							annotation.method,
							"${controller.path}${annotation.path}",
							function,
							controllerInstance
						)
					}
				}
			}
		}
		scanResult.close()
	}
}

private fun registerRoute(
	route: Route,
	method: HttpMethod,
	path: String,
	function: KFunction<*>,
	controllerInstance: Any
) {
	val routeHandler: suspend (ApplicationCall) -> Unit = {
		it.invokeControllerMethod(function, controllerInstance)
	}

	val routeBuilder: Route.() -> Unit = {
		when (method) {
			HttpMethod.GET -> get(path) { routeHandler(call) }
			HttpMethod.POST -> post(path) { routeHandler(call) }
			HttpMethod.PUT -> put(path) { routeHandler(call) }
			HttpMethod.PATCH -> patch(path) { routeHandler(call) }
			HttpMethod.DELETE -> delete(path) { routeHandler(call) }
		}
	}

	if (function.findAnnotation<Authenticated>() != null) {
		route.authenticate {
			routeBuilder()
		}
	} else {
		route.routeBuilder()
	}
}
