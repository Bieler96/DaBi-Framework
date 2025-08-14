package di

import di.annotations.Injectable
import di.annotations.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.hasAnnotation

/**
 * Container für die Dependency Injection.
 */
class Container {
    private val instances = mutableMapOf<KClass<*>, Any>()

    /**
     * Registriert eine Instanz für einen bestimmten Typ.
     */
    fun <T : Any> register(type: KClass<T>, instance: T) {
        instances[type] = instance
    }

    /**
     * Interne Methode für die direkte Registrierung ohne generische Parameter.
     * Wird von der DSL verwendet.
     */
    internal fun directRegister(type: KClass<*>, instance: Any) {
        instances[type] = instance
    }

    /**
     * Registriert eine Klasse als Injectable.
     */
    inline fun <reified T : Any> register() {
        register(T::class)
    }

    /**
     * Registriert eine Klasse als Injectable.
     */
    fun <T : Any> register(type: KClass<T>) {
        if (!type.hasAnnotation<Injectable>()) {
            throw IllegalArgumentException("Klasse ${type.simpleName} ist nicht mit @Injectable annotiert")
        }

        // Lazy initialization - wird nur registriert, aber erst bei Bedarf instanziiert
        instances.computeIfAbsent(type) { resolveClass(type) }
    }

    /**
     * Holt eine Instanz vom Container. Instanziiert die Klasse bei Bedarf.
     */
    inline fun <reified T : Any> get(): T = get(T::class)

    /**
     * Holt eine Instanz vom Container. Instanziiert die Klasse bei Bedarf.
     * Registriert die Klasse automatisch, falls sie noch nicht registriert ist.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): T {
        val instance = instances[type] ?: resolveClass(type)
        return instance as T
    }

    /**
     * Löst die Abhängigkeiten auf und erstellt eine Instanz.
     */
    private fun <T : Any> resolveClass(type: KClass<T>): Any {
        if (!type.hasAnnotation<Injectable>()) {
            throw IllegalArgumentException("Klasse ${type.simpleName} ist nicht mit @Injectable annotiert")
        }

        val constructor = type.primaryConstructor 
            ?: type.constructors.find { it.hasAnnotation<Inject>() }
            ?: throw IllegalArgumentException("Klasse ${type.simpleName} hat keinen Konstruktor mit @Inject oder keinen Standard-Konstruktor")

        // Konstruktor-Parameter auflösen
        val parameters = constructor.parameters
        val arguments = parameters.associateWith { parameter ->
            resolveParameter(parameter)
        }

        return constructor.callBy(arguments)
    }

    /**
     * Löst einen Parameter auf, indem eine Instanz dafür aus dem Container geholt wird.
     */
    private fun resolveParameter(parameter: KParameter): Any? {
        val parameterType = parameter.type.classifier as? KClass<*>
            ?: throw IllegalArgumentException("Konnte Typ für Parameter ${parameter.name} nicht auflösen")

        if (parameter.isOptional && !instances.containsKey(parameterType)) {
            return null
        }

        return instances[parameterType] ?: resolveClass(parameterType)
    }

    companion object {
        // Singleton-Instanz für einfachen globalen Zugriff
        val global = Container()
    }
}