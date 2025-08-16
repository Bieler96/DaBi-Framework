package utils.annotations

import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Annotation zur Erkennung von Ausreißern in einem Datensatz.
 *
 * @param threshold Der Schwellenwert für die Erkennung von Ausreißern.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class OutlierDetection(val threshold: Double = 2.0)

/**
 * Funktion zur Erkennung von Ausreißern in einer Liste von Objekten.
 *
 * @param data Die Liste von Objekten, die analysiert werden sollen.
 * @param clazz Die Klasse der Objekte in der Liste.
 * @param T Der Typ der Objekte in der Liste.
 */
fun <T : Any> detectOutliers(data: List<T>, clazz: KClass<T>) {
	val fields = clazz.members.filterIsInstance<KProperty1<T, *>>()
	fields.forEach { field ->
		val annotation = field.annotations.filterIsInstance<OutlierDetection>().firstOrNull()
		if (annotation != null) {
			val values = data.mapNotNull { field.get(it) as? Double }
			val mean = values.average()
			val stdDev = kotlin.math.sqrt(values.map { (it - mean).pow(2) }.average())

			val outliers = values.filter { kotlin.math.abs(it - mean) > annotation.threshold * stdDev }
			println("Ausreißer in ${field.name}: $outliers")
		}
	}
}