package dabiserverextension

import auth.AuthController
import di.DI
import io.github.classgraph.ClassGraph
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

fun Application.registerAnnotatedRoutes(
	vararg controllersPackages: String = arrayOf("de.bieler.controller")
) {
	routing {
		// Passe das Package an, in dem sich deine Controller befinden
		val scanResult = ClassGraph()
			.enableAllInfo()
			.acceptPackages(*controllersPackages)
			.scan()

		val controllerClasses = scanResult.allClasses

		for (classInfo in controllerClasses) {
			val kClass = Class.forName(classInfo.name).kotlin

			// Nur Controller-Klassen berücksichtigen
			if (!kClass.annotations.any { it is Controller }) continue

			val controllerInstance = when (kClass) {
				AuthController::class -> AuthController(DI.get())
				else -> kClass.createInstance()
			}

			val controller = kClass.annotations.find { it is Controller } as Controller

			for (function in kClass.declaredFunctions) {
				// Beispiel: Behandlung von @PostMapping
				function.findAnnotation<PostMapping>()?.let { mapping ->
					post("${controller.path}${mapping.path}") {
						call.invokeControllerMethod(function, controllerInstance)
					}
				}
				// Analog für @GetMapping, @PutMapping, @PatchMapping, @RequestMapping etc.
				function.findAnnotation<GetMapping>()?.let { mapping ->
					get("${controller.path}${mapping.path}") {
						call.invokeControllerMethod(function, controllerInstance)
					}
				}
				function.findAnnotation<PutMapping>()?.let { mapping ->
					put("${controller.path}${mapping.path}") {
						call.invokeControllerMethod(function, controllerInstance)
					}
				}
				function.findAnnotation<PatchMapping>()?.let { mapping ->
					patch("${controller.path}${mapping.path}") {
						call.invokeControllerMethod(function, controllerInstance)
					}
				}
				function.findAnnotation<DeleteMapping>()?.let { mapping ->
					delete("${controller.path}${mapping.path}") {
						call.invokeControllerMethod(function, controllerInstance)
					}
				}
				function.findAnnotation<RequestMapping>()?.let { mapping ->
					when (mapping.method) {
						HttpMethod.GET -> get("${controller.path}${mapping.path}") {
							call.invokeControllerMethod(
								function,
								controllerInstance
							)
						}

						HttpMethod.POST -> post("${controller.path}${mapping.path}") {
							call.invokeControllerMethod(
								function,
								controllerInstance
							)
						}

						HttpMethod.PUT -> put("${controller.path}${mapping.path}") {
							call.invokeControllerMethod(
								function,
								controllerInstance
							)
						}

						HttpMethod.PATCH -> patch("${controller.path}${mapping.path}") {
							call.invokeControllerMethod(
								function,
								controllerInstance
							)
						}

						HttpMethod.DELETE -> delete("${controller.path}${mapping.path}") {
							call.invokeControllerMethod(
								function,
								controllerInstance
							)
						}

						else -> {}
					}
				}
			}
		}
		scanResult.close()
	}
}