package utils.annotations

import kotlin.reflect.KFunction

/**
 * Annotation um die Ausführungszeit einer Methode zu messen.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MeasureTime()

/**
 * Extension-Funktion, um die Ausführungszeit einer Methode zu messen.
 *
 * @param method Die Methode, deren Ausführungszeit gemessen werden soll.
 * @param block Der Codeblock, dessen Ausführungszeit gemessen werden soll.
 */
inline fun <reified T> T.measureTime(method: KFunction<*>, block: () -> Unit) {
	val start = System.nanoTime()
	block()
	val end = System.nanoTime()
	println("${method.name} dauerte ${(end - start) / 1_000_000}ms")
}