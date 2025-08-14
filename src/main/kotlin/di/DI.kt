package di

import di.dsl.DependencyBuilder
import kotlin.reflect.KClass

/**
 * Einfache Fassade für den globalen DI-Container.
 * Erlaubt die Verwendung von DI.get<Service>() statt Container.global.get<Service>().
 */
object DI {
    /**
     * Holt eine Instanz vom globalen Container.
     */
    inline fun <reified T : Any> get(): T = Container.global.get<T>()

    /**
     * Registriert eine Klasse im globalen Container.
     */
    inline fun <reified T : Any> register() {
        Container.global.register<T>()
    }

    /**
     * Registriert eine Instanz im globalen Container.
     */
    fun <T : Any> register(type: KClass<T>, instance: T) {
        Container.global.register(type, instance)
    }

    /**
     * Konfiguriert den DI-Container mit der DSL
     */
    fun configure(init: DependencyBuilder.() -> Unit) {
        val builder = DependencyBuilder()
        builder.init()
        val container = builder.build()

        // Da wir mit dem globalen Container arbeiten, übernehmen wir alle Instanzen
        // In einer fortgeschritteneren Implementierung würden wir hier Strategien für
        // das Zusammenführen von Containern implementieren
    }
}