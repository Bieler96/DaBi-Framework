package dataframe

data class ScoredCriterion<T : Enum<T>>(
    val criterion: T,
    val weight: Double,
    val value: Double
)

class GenericScoringBuilder<T : Enum<T>> {
    private val criteria = mutableListOf<ScoredCriterion<T>>()

    fun criterion(criterion: T, weight: Double, value: Double) {
        criteria.add(ScoredCriterion(criterion, weight, value))
    }

    fun build(): Double {
        val totalWeight = criteria.sumOf { it.weight }.takeIf { it > 0 } ?: 1.0
        return criteria.sumOf { (it.weight / totalWeight) * it.value }
    }

    fun resultMap(): Map<T, Double> {
        val totalWeight = criteria.sumOf { it.weight }.takeIf { it > 0 } ?: 1.0
        return criteria.associate { it.criterion to (it.weight / totalWeight) * it.value }
    }
}

inline fun <reified T : Enum<T>> scoring(
    block: GenericScoringBuilder<T>.() -> Unit
): Pair<Double, Map<T, Double>> {
    val builder = GenericScoringBuilder<T>()
    builder.block()
    return builder.build() to builder.resultMap()
}
