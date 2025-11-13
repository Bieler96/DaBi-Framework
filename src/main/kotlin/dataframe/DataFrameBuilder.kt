package dataframe

class DataFrameBuilder {
	private val columns = mutableListOf<String>()
	private val rows = mutableListOf<List<Any?>>()

	fun columns(vararg names: String) {
		columns.addAll(names)
	}

	fun addRow(vararg values: Any?) {
		rows.add(values.toList())
	}

	fun build(): DataFrame {
		val df = DataFrame(columns)
		for (row in rows) {
			df.addRow(*row.toTypedArray())
		}
		return df
	}
}