package dataframe

import java.io.File

private fun parseValue(value: String): Any? {
	val trimmed = value.trim()
	if (trimmed.equals("null", ignoreCase = true)) return null
	return when {
		trimmed.toIntOrNull() != null -> trimmed.toInt()
		trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
		trimmed.equals("true", ignoreCase = true) -> true
		trimmed.equals("false", ignoreCase = true) -> false
		else -> trimmed
	}
}

fun read(file: File, separator: Char = ','): DataFrame {
	val lines = file.readLines().filter { it.isNotBlank() }
	val header = lines.first().split(separator).map { it.trim() }
	val df = DataFrame(header)

	for (line in lines.drop(1)) {
		val values = line.split(separator).map { parseValue(it) }
		df.addRow(*values.toTypedArray())
	}

	return df
}