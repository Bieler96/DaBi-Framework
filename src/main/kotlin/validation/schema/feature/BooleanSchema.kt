package validation.schema.feature

import validation.error.TypeError
import validation.schema.dsl.Schema

class BooleanSchema : Schema<Boolean> {
	private var isOptional = false
	private var isNullable = false
	private var defaultValue: Boolean? = null

	fun optional(): BooleanSchema {
		isOptional = true
		return this
	}

	fun nullable(): BooleanSchema {
		isNullable = true
		return this
	}

	fun default(value: Boolean): BooleanSchema {
		defaultValue = value
		isOptional = true
		return this
	}

	override fun parse(value: Any?): Boolean {
		if (value == null) {
			if (isNullable) return value as Boolean
			if (isOptional) return defaultValue ?: throw TypeError("Missing value")
			throw TypeError("Value is null")
		}

		return value as? Boolean ?: throw TypeError("boolean: Expected a Boolean, but got ${value::class.simpleName}")
	}
}