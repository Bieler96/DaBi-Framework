package utils.annotations

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Annotation zur Erkennung von Trends in einem Datensatz.
 *
 * @param threshold Der Schwellenwert für die Erkennung von Trends.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrendDetection

/**
 * Funktion zur Erkennung von Trends in einer Liste von Objekten.
 *
 * @param data Die Liste von Objekten, die analysiert werden sollen.
 * @param clazz Die Klasse der Objekte in der Liste.
 * @param T Der Typ der Objekte in der Liste.
 */
fun <T : Any> detectTrend(data: List<T>, clazz: KClass<T>) {
	val fields = clazz.members.filterIsInstance<KProperty1<T, *>>()
	fields.forEach { field ->
		val annotation = field.annotations.filterIsInstance<TrendDetection>().firstOrNull()
		if (annotation != null) {
			val values = data.mapNotNull { field.get(it) as? Double }
			val diffs = values.zipWithNext { a, b -> b - a }
			val trend = when {
				diffs.all { it > 0 } -> "Steigend 📈"
				diffs.all { it < 0 } -> "Fallend 📉"
				else -> "Schwankend ↔️"
			}
			println("Trend für ${field.name}: $trend")
		}
	}
}
