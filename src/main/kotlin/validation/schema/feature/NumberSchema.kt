package validation.schema.feature

import validation.error.CheckError
import validation.error.TypeError
import validation.schema.core.Schema

class NumberSchema : Schema<Number> {
	private val checks = mutableListOf<(Number) -> Unit>()
	private var isOptional = false
	private var isNullable = false
	private var defaultValue: Number? = null

	fun min(minValue: Number): NumberSchema {
		checks.add { if (it.toDouble() < minValue.toDouble()) throw CheckError("Number too small") }
		return this
	}

	fun max(maxValue: Number): NumberSchema {
		checks.add { if (it.toDouble() > maxValue.toDouble()) throw CheckError("Number too large") }
		return this
	}

	fun lessThan(value: Number): NumberSchema {
		checks.add { if (it.toDouble() >= value.toDouble()) throw CheckError("Number is not less than $value") }
		return this
	}

	fun greaterThan(value: Number): NumberSchema {
		checks.add { if (it.toDouble() <= value.toDouble()) throw CheckError("Number is not greater than $value") }
		return this
	}

	fun int(): NumberSchema {
		checks.add { if (it !is Int) throw CheckError("Number is not an integer") }
		return this
	}

	fun float(): NumberSchema {
		checks.add { if (it !is Float) throw CheckError("Number is not a float") }
		return this
	}

	fun double(): NumberSchema {
		checks.add { if (it !is Double) throw CheckError("Number is not a double") }
		return this
	}

	fun positive(): NumberSchema {
		checks.add { if (it.toDouble() <= 0) throw CheckError("Number is not positive") }
		return this
	}

	fun negative(): NumberSchema {
		checks.add { if (it.toDouble() > 0) throw CheckError("Number is not negative") }
		return this
	}

	fun even(): NumberSchema {
		checks.add { if (it.toInt() % 2 != 0) throw CheckError("Number is not even") }
		return this
	}

	fun odd(): NumberSchema {
		checks.add { if (it.toInt() % 2 == 0) throw CheckError("Number is not odd") }
		return this
	}

	fun between(min: Number, max: Number): NumberSchema {
		checks.add { if (it.toDouble() < min.toDouble() || it.toDouble() > max.toDouble()) throw CheckError("Number is not between $min and $max") }
		return this
	}

	fun multipleOf(value: Number): NumberSchema {
		checks.add { if (it.toDouble() % value.toDouble() != 0.0) throw CheckError("Number is not a multiple of $value") }
		return this
	}

	fun optional(): NumberSchema {
		isOptional = true
		return this
	}

	fun nullable(): NumberSchema {
		isNullable = true
		return this
	}

	fun default(value: Number): NumberSchema {
		defaultValue = value
		return this
	}

	override fun parse(value: Any?): Number? {
		if (value == null) {
			if (isNullable) return value as Number?
			if (isOptional) return defaultValue ?: throw TypeError("Missing value")
			throw TypeError("Value is null")
		}

		val num = when (value) {
			is Number -> value
			is String -> {
				val trimmed = value.trim()
				val intValue = trimmed.toIntOrNull()
				if (intValue != null) {
					intValue
				} else {
					val doubleValue = trimmed.toDoubleOrNull()
					if (doubleValue != null) {
						// Prüfen, ob doubleValue außerhalb des Int-Bereichs liegt oder eine Nachkommastelle hat
						if (doubleValue % 1.0 != 0.0 || doubleValue > Int.MAX_VALUE || doubleValue < Int.MIN_VALUE) {
							doubleValue
						} else {
							doubleValue.toInt()
						}
					} else {
						throw TypeError("number: Expected a Number or numeric String, but got String [value: $value]")
					}
				}
			}
			else -> throw TypeError("number: Expected a Number, but got \\${value::class.simpleName} [value: $value]")
		}
		checks.forEach { it(num) }
		return num
	}
}