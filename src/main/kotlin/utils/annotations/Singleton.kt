package utils.annotations

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Annotation, die angibt, dass eine Klasse als Singleton behandelt werden soll.
 * Diese Annotation wird verwendet, um sicherzustellen, dass nur eine Instanz der Klasse existiert.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

/**
 * Ein Registrierungsobjekt für Singleton-Instanzen.
 * Dieses Objekt ist verantwortlich für die Verwaltung des Lebenszyklus von Singleton-Instanzen.
 */
object SingletonRegistry {
	private val instances = mutableMapOf<KClass<*>, Any>()

	/**
	 * Gibt die Singleton-Instanz der angegebenen Klasse zurück.
	 * Wenn die Instanz nicht existiert, wird eine neue erstellt und im Registrierungsobjekt gespeichert.
	 *
	 * @param T Der Typ der Singleton-Instanz.
	 * @return Die Singleton-Instanz der angegebenen Klasse.
	 */
	internal inline fun <reified T : Any> getInstance(): T {
		return instances.getOrPut(T::class) { T::class.createInstance() } as T
	}
}