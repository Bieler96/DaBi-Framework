package dataframe

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlinx.jupyter.api.Renderable
import java.io.File
import org.jetbrains.kotlinx.jupyter.api.DisplayResult
import org.jetbrains.kotlinx.jupyter.api.Notebook

fun dataframe(block: DataFrameBuilder.() -> Unit): DataFrame {
	val builder = DataFrameBuilder()
	builder.block()
	return builder.build()
}

enum class JoinType {
	INNER,
	LEFT,
	RIGHT,
	OUTER
}

class DataFrame(private val columns: List<String>) : Renderable {
	companion object {
		fun fromCSV(filePath: String): DataFrame {
			val lines = File(filePath).readLines()
			val header = lines.first().split(',')
			val df = DataFrame(header)

			lines.drop(1).forEach { line ->
				val values = line.split(',').map {
					it.toIntOrNull() ?: it.toDoubleOrNull() ?: it
				}
				df.addRow(*values.toTypedArray())
			}
			return df
		}

		fun fromJSON(filePath: String): DataFrame {
			val fileContent = File(filePath).readText()
			val json = Json.parseToJsonElement(fileContent).jsonArray
			val columns = json.first().jsonObject.keys.toList()
			val df = DataFrame(columns)

			json.forEach { element ->
				val row = columns.map { col ->
					val value = element.jsonObject[col]?.jsonPrimitive?.content
					value?.toIntOrNull() ?: value?.toDoubleOrNull() ?: value
				}
				df.addRow(*row.toTypedArray())
			}

			return df
		}
	}

	val rows: MutableList<List<Any?>> = mutableListOf()

	fun addRow(vararg values: Any?) {
		require(values.size == columns.size) { "Row must have the same number of values as columns" }
		rows.add(values.toList())
	}

	fun getColumnNames(): List<String> = columns

	fun getColumn(name: String): List<Any?> {
		val index = columns.indexOf(name)
		require(index != -1) { "Column $name not found" }
		return rows.map { it[index] }
	}

	inner class Row(private val values: List<Any?>) {
		operator fun get(columnName: String): Any? {
			val index = columns.indexOf(columnName)
			require(index != -1) { "Column '$columnName' not found" }
			return values[index]
		}

		operator fun get(columnIndex: Int): Any? {
			require(columnIndex >= 0 && columnIndex < values.size) { "Index $columnIndex is out of bounds" }
			return values[columnIndex]
		}
	}

	fun filter(predicate: (Row) -> Boolean): DataFrame {
		val filtered = DataFrame(columns)
		filtered.rows.addAll(rows.filter { predicate(Row(it)) })
		return filtered
	}

	fun map(transform: (List<Any?>) -> List<Any?>): DataFrame {
		val mapped = DataFrame(columns)
		mapped.rows.addAll(rows.map(transform))
		return mapped
	}

	fun groupBy(column: String): GroupedDataFrame {
		if (column !in columns) throw IllegalArgumentException("Column $column does not exist")
		val grouped = rows.groupBy { it[columns.indexOf(column)] }.mapValues { (_, groupRows) ->
			DataFrame(columns).also { it.rows.addAll(groupRows) }
		}
		return GroupedDataFrame(grouped)
	}

	fun sum(column: String): Double {
		val numbers = getColumn(column).filterIsInstance<Number>()
		return numbers.sumOf { it.toDouble() }
	}

	fun avg(column: String): Double {
		val numbers = getColumn(column).filterIsInstance<Number>()
		return if (numbers.isEmpty()) 0.0 else numbers.sumOf { it.toDouble() } / numbers.size
	}

	fun min(column: String): Double? {
		val numbers = getColumn(column).filterIsInstance<Number>()
		return numbers.minOfOrNull { it.toDouble() }
	}

	fun max(column: String): Double? {
		val numbers = getColumn(column).filterIsInstance<Number>()
		return numbers.maxOfOrNull { it.toDouble() }
	}

	fun aggregate(column: String, block: (List<Any?>) -> Any): Any {
		val values = getColumn(column)
		return block(values)
	}

	val rowCount: Int get() = rows.size

	private fun rowToMap(row: List<Any?>): Map<String, Any?> {
		return columns.zip(row).toMap()
	}

	fun withColumn(name: String, transform: (Map<String, Any?>) -> Any?): DataFrame {
		val existingColumnIndex = columns.indexOf(name)
		val newColumns = if (existingColumnIndex == -1) columns + name else columns
		val newDf = DataFrame(newColumns)

		rows.forEach { row ->
			val rowMap = rowToMap(row)
			val newValue = transform(rowMap)
			val newRow = if (existingColumnIndex == -1) {
				row + newValue
			} else {
				row.toMutableList().also { it[existingColumnIndex] = newValue }
			}
			newDf.addRow(*newRow.toTypedArray())
		}
		return newDf
	}

	fun select(vararg selectedColumns: String): DataFrame {
		val missing = selectedColumns.filter { it !in columns }
		require(missing.isEmpty()) { "Columns not found: ${missing.joinToString()}" }

		val newIndices = selectedColumns.map { columns.indexOf(it) }
		val newDf = DataFrame(selectedColumns.toList())

		rows.forEach { row ->
			val newRow = newIndices.map { row[it] }
			newDf.addRow(*newRow.toTypedArray())
		}

		return newDf
	}

	fun join(other: DataFrame, on: String, type: JoinType = JoinType.INNER): DataFrame {
		require(on in this.columns) { "Join column '$on' not found in left DataFrame." }
		require(on in other.columns) { "Join column '$on' not found in right DataFrame." }

		return when (type) {
			JoinType.INNER -> innerJoin(other, on)
			JoinType.LEFT -> leftJoin(other, on)
			JoinType.RIGHT -> other.leftJoin(this, on) // Swapped receiver and argument
			JoinType.OUTER -> outerJoin(other, on)
		}
	}

	private fun innerJoin(other: DataFrame, on: String): DataFrame {
		val leftJoinColIndex = this.columns.indexOf(on)
		val rightJoinColIndex = other.columns.indexOf(on)
		val newColumns = this.columns + other.columns.filter { it != on }
		val result = DataFrame(newColumns)
		val rightMap = other.rows.associateBy { it[rightJoinColIndex] }

		for (leftRow in this.rows) {
			val joinValue = leftRow[leftJoinColIndex]
			rightMap[joinValue]?.let { rightRow ->
				val newRow = leftRow + rightRow.filterIndexed { index, _ -> index != rightJoinColIndex }
				result.addRow(*newRow.toTypedArray())
			}
		}
		return result
	}

	private fun leftJoin(other: DataFrame, on: String): DataFrame {
		val leftJoinColIndex = this.columns.indexOf(on)
		val rightJoinColIndex = other.columns.indexOf(on)
		val newColumns = this.columns + other.columns.filter { it != on }
		val result = DataFrame(newColumns)
		val rightMap = other.rows.associateBy { it[rightJoinColIndex] }

		for (leftRow in this.rows) {
			val joinValue = leftRow[leftJoinColIndex]
			val rightRow = rightMap[joinValue]
			val newRow = if (rightRow != null) {
				leftRow + rightRow.filterIndexed { index, _ -> index != rightJoinColIndex }
			} else {
				leftRow + List(other.columns.size - 1) { null }
			}
			result.addRow(*newRow.toTypedArray())
		}
		return result
	}

	private fun outerJoin(other: DataFrame, on: String): DataFrame {
		val leftResult = this.leftJoin(other, on)
		val rightResult = other.leftJoin(this, on)

		val leftJoinColIndex = this.columns.indexOf(on)
		val rightJoinColIndex = rightResult.columns.indexOf(on)

		val leftKeys = this.rows.map { it[leftJoinColIndex] }.toSet()
		val rowsToAdd = rightResult.rows.filter { it[rightJoinColIndex] !in leftKeys }

		rowsToAdd.forEach {
			leftResult.addRow(*it.toTypedArray())
		}

		return leftResult
	}

	fun fillNa(value: Any): DataFrame {
		val newDf = DataFrame(columns)
		rows.forEach { row ->
			val newRow = row.map { it ?: value }
			newDf.addRow(*newRow.toTypedArray())
		}
		return newDf
	}

	fun dropNa(): DataFrame {
		val newDf = DataFrame(columns)
		rows.forEach { row ->
			if (row.none { it == null }) {
				newDf.addRow(*row.toTypedArray())
			}
		}
		return newDf
	}

	fun orderBy(vararg columns: String, ascending: Boolean = true): DataFrame {
		val sortPairs = columns.map { it to ascending }.toTypedArray()
		return orderBy(*sortPairs)
	}

	fun orderBy(vararg sortSpecs: Pair<String, Boolean>): DataFrame {
		val missing = sortSpecs.map { it.first }.filter { it !in columns }
		require(missing.isEmpty()) { "Columns not found: ${missing.joinToString()}" }

		val sortIndices = sortSpecs.map { (col, asc) ->
			Triple(columns.indexOf(col), if (asc) 1 else -1, asc)
		}

		val comparator = Comparator<List<Any?>> { a, b ->
			for ((index, direction, asc) in sortIndices) {
				val v1 = a[index]
				val v2 = b[index]

				val comparison = when {
					v1 is Comparable<*> && v2 is Comparable<*> -> (v1 as Comparable<Any>).compareTo(v2)
					v1 == null && v2 == null -> 0
					v1 == null -> if (asc) -1 else 1
					v2 == null -> if (asc) 1 else -1
					else -> v1.toString().compareTo(v2.toString())
				}

				if (comparison != 0) {
					return@Comparator comparison * direction
				}
			}
			0
		}

		val newDf = DataFrame(this.columns)
		newDf.rows.addAll(this.rows.sortedWith(comparator))
		return newDf
	}

	//	override fun toString(): String {
//		val builder = StringBuilder()
//		builder.appendLine(columns.joinToString(" | "))
//		builder.appendLine("-".repeat(columns.size * 10))
//		for (row in rows) {
//			builder.appendLine(row.joinToString(" | ") { it.toString() })
//		}
//		return builder.toString()
//	}
	override fun render(notebook: Notebook): DisplayResult {
		val table = MyTable(columns, rows.map { row -> row.map { it ?: "null" } })
		return table.render(notebook)
	}
}
