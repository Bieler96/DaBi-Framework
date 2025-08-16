package utils.annotations

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Annotation zur Normalisierung von Daten.
 *
 * @param min Der minimale Wert für die Normalisierung.
 * @param max Der maximale Wert für die Normalisierung.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Normalize

/**
 * Funktion zur Normalisierung von Daten in einer Liste von Objekten.
 *
 * @param data Die Liste von Objekten, die normalisiert werden sollen.
 * @param clazz Die Klasse der Objekte in der Liste.
 * @param T Der Typ der Objekte in der Liste.
 */
fun <T : Any> normalizeData(data: List<T>, clazz: KClass<T>) {
	val fields = clazz.members.filterIsInstance<KProperty1<T, *>>()
	fields.forEach { field ->
		val annotation = field.annotations.filterIsInstance<Normalize>().firstOrNull()
		if (annotation != null) {
			val values = data.mapNotNull { field.get(it) as? Double }
			val min = values.minOrNull() ?: return
			val max = values.maxOrNull() ?: return

			val normalizedValues = values.map { (it - min) / (max - min) }
			println("Normalisierte Werte für ${field.name}: $normalizedValues")
		}
	}
}