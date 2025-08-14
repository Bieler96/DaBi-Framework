package di.dsl

import di.Container
import kotlin.reflect.KClass

/**
 * DSL Builder für die Dependency Injection Konfiguration
 */
class DependencyBuilder {
    val dependencies = mutableMapOf<KClass<*>, () -> Any>()

    inline fun <reified T : Any> singleton(noinline provider: () -> T) {
        dependencies[T::class] = provider as () -> Any
    }

    inline fun <reified T : Any> factory(noinline provider: () -> T) {
        dependencies[T::class] = provider as () -> Any
    }

    fun build(): Container {
        val container = Container()
        dependencies.forEach { (kClass, provider) ->
            val instance = provider()
            // Direkter Zugriff auf instances-Map über direkte Zuweisung statt generischer Methode
            container.directRegister(kClass, instance)
        }
        return container
    }
}

/**
 * Einstiegspunkt für die DSL
 */
fun dependencies(init: DependencyBuilder.() -> Unit): Container {
    val builder = DependencyBuilder()
    builder.init()
    return builder.build()
}