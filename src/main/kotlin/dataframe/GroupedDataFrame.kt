package dataframe

import kotlin.collections.iterator

class GroupedDataFrame(private val groups: Map<Any?, DataFrame>) {
	fun count(): Map<Any?, Int> {
		return groups.mapValues { it.value.rowCount }
	}

	fun sum(column: String): Map<Any?, Double> {
		return groups.mapValues { (_, df) ->
			df.getColumn(column).filterIsInstance<Number>().sumOf { it.toDouble() }
		}
	}

	fun avg(column: String): Map<Any?, Double> {
		return groups.mapValues { (_, df) ->
			val numbers = df.getColumn(column).filterIsInstance<Number>()
			if (numbers.isEmpty()) 0.0 else numbers.sumOf { it.toDouble() } / numbers.size
		}
	}

	fun min(column: String): Map<Any?, Double?> {
		return groups.mapValues { (_, df) ->
			df.getColumn(column).filterIsInstance<Number>().minOfOrNull { it.toDouble() }
		}
	}

	fun max(column: String): Map<Any?, Double?> {
		return groups.mapValues { (_, df) ->
			df.getColumn(column).filterIsInstance<Number>().maxOfOrNull { it.toDouble() }
		}
	}

	fun aggregate(column: String, block: (List<Any?>) -> Any): DataFrame {
		val keyName = "group"
		val valueName = "value"

		val result = dataframe {
			columns(keyName, valueName)
		}

		groups.forEach { (key, df) ->
			val values = df.getColumn(column)
			val aggregatedValue = block(values)
			result.addRow(key, aggregatedValue)
		}

		return result
	}

	fun agg(vararg aggregations: Pair<String, String>): DataFrame {
	    val groupKeyName = groups.values.firstOrNull()?.getColumnNames()?.firstOrNull() ?: "group"

	    val newColumns = mutableListOf(groupKeyName)
	    aggregations.forEach { (col, agg) -> newColumns.add("${col}_${agg}") }

	    val result = DataFrame(newColumns)

	    val aggFuncs: Map<String, (DataFrame, String) -> Any?> = mapOf(
	        "sum" to { df, col -> df.sum(col) },
	        "avg" to { df, col -> df.avg(col) },
	        "min" to { df, col -> df.min(col) },
	        "max" to { df, col -> df.max(col) },
	        "count" to { df, _ -> df.rowCount }
	    )

	    for ((key, df) in groups) {
	        val newRow = mutableListOf<Any?>()
	        newRow.add(key)
	        for ((col, agg) in aggregations) {
	            val func = aggFuncs[agg] ?: throw IllegalArgumentException("Unknown aggregation: $agg")
	            newRow.add(func(df, col))
	        }
	        result.addRow(*newRow.toTypedArray())
	    }

	    return result
	}

	override fun toString(): String {
		val builder = StringBuilder()

		for ((key, df) in groups) {
			builder.appendLine("Group: $key")
			builder.appendLine(df.toString())
		}

		return builder.toString()
	}
}