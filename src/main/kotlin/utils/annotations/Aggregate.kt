package utils.annotations

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Annotation zum Aggregieren von Werten in einer Liste.
 *
 * @param type Der Typ der Aggregation (z.B. "sum", "avg").
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Aggregate(val type: String = "sum")

/**
 * Aggregiert Werte in einer Liste basierend auf der angegebenen Annotation.
 *
 * @param data Die Liste von Objekten, die aggregiert werden sollen.
 * @param clazz Die Klasse der Objekte in der Liste.
 * @param T Der Typ der Objekte in der Liste.
 * @return Eine Map mit den aggregierten Werten.
 */
fun <T : Any> aggregate(data: List<T>, clazz: KClass<T>): Map<String, Any?> {
	val fields = clazz.members.filterIsInstance<KProperty1<T, *>>()
	val results = mutableMapOf<String, Any?>()

	fields.forEach { field ->
		val annotation = field.annotations.filterIsInstance<Aggregate>().firstOrNull()
		if (annotation != null) {
			val values = data.mapNotNull { field.get(it) as? Number }
			val result = when (annotation.type) {
				"sum" -> values.sumOf { it.toDouble() }
				"avg" -> values.map { it.toDouble() }.average()
				else -> null
			}
			results[field.name] = result
			println("${field.name} (${annotation.type}): $result")
		}
	}

	return results
}