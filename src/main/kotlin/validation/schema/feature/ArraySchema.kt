package validation.schema.feature

import validation.error.CheckError
import validation.error.TypeError
import validation.schema.dsl.Schema

class ArraySchema<T>(private val itemSchema: Schema<T>) : Schema<List<T>> {
	private val checks = mutableListOf<(List<T>) -> Unit>()
	private var isNullable = false

	fun min(length: Int): ArraySchema<T> {
		checks.add { if (it.size < length) throw CheckError("Array too short") }
		return this
	}

	fun max(length: Int): ArraySchema<T> {
		checks.add { if (it.size > length) throw CheckError("Array too long") }
		return this
	}

	fun length(length: Int): ArraySchema<T> {
		checks.add { if (it.size != length) throw CheckError("Array has incorrect length") }
		return this
	}

	override fun parse(value: Any?): List<T>? {
		if (value == null) {
			if (isNullable) return null
			throw TypeError("Value is null")
		}

		val list = value as? List<*> ?: throw TypeError("array: Expected a list, but got ${value!!::class.simpleName}")
		return list.map { itemSchema.parse(it) as T }
	}
}