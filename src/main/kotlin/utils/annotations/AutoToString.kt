package utils.annotations

/**
 * Annotation zur automatischen Generierung einer toString-Methode für eine Klasse.
 * Diese Annotation sollte auf Klassen angewendet werden.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoToString

/**
 * Erweiterungsfunktion, die eine toString-Implementierung für eine Klasse generiert,
 * die mit der @AutoToString-Annotation versehen ist.
 *
 * @return Eine String-Darstellung der Klasse mit ihren Eigenschaften und Werten.
 */
inline fun <reified T> T.autoToString(): String {
	val properties = T::class.members.filterIsInstance<kotlin.reflect.KProperty1<T, *>>()
	return properties.joinToString(", ", prefix = "${T::class.simpleName}(", postfix = ")") {
		"${it.name}=${it.get(this)}"
	}
}