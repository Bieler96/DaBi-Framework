package utils.annotations

/**
 * Annotation um eine Zeitreihe zu kennzeichnen.
 *
 * @TimeSeries
 * data class StockPrice(val timestamp: Long, val price: Double)
 *
 * val data = listOf(
 *     StockPrice(1711825200, 150.5),
 *     StockPrice(1711911600, 152.0)
 * )
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TimeSeries