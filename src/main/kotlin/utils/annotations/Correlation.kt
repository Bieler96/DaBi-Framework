package utils.annotations

import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * Annotation zur Berechnung der Korrelation zwischen zwei Feldern in einer Liste von Objekten.
 *
 * @param fieldX Der Name des ersten Feldes.
 * @param fieldY Der Name des zweiten Feldes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Correlation(
	val fieldX: String,
	val fieldY: String
)

/**
 * Berechnet die Korrelation zwischen zwei Feldern in einer Liste von Objekten.
 *
 * @param data Die Liste von Objekten, die analysiert werden sollen.
 * @param clazz Die Klasse der Objekte in der Liste.
 * @param T Der Typ der Objekte in der Liste.
 * @return Der Korrelationskoeffizient oder NaN, wenn die Berechnung nicht möglich ist.
 */
fun <T : Any> computeCorrelation(data: List<T>, clazz: KClass<T>): Double {
	val annotation = clazz.annotations.filterIsInstance<Correlation>().firstOrNull() ?: return Double.NaN
	val xValues = data.mapNotNull { clazz.members.find { it.name == annotation.fieldX }?.call(it) as? Double }
	val yValues = data.mapNotNull { clazz.members.find { it.name == annotation.fieldY }?.call(it) as? Double }

	if (xValues.size != yValues.size || xValues.isEmpty()) return Double.NaN

	val xMean = xValues.average()
	val yMean = yValues.average()

	val numerator = xValues.zip(yValues).sumOf { (x, y) -> (x - xMean) * (y - yMean) }
	val denominator = kotlin.math.sqrt(xValues.sumOf { (it - xMean).pow(2) }) *
			kotlin.math.sqrt(yValues.sumOf { (it - yMean).pow(2) })

	return if (denominator == 0.0) Double.NaN else numerator / denominator
}
