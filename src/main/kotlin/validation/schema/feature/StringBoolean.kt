package validation.schema.feature

import validation.schema.dsl.Schema

class StringBoolean : Schema<Boolean> {
	private var truthyList = listOf(
		"true",
		"1",
		"yes",
		"on",
		"y",
		"enabled",
		"ok",
		"confirm",
	)
	private var falsyList = listOf(
		"false",
		"0",
		"no",
		"n",
		"disabled",
		"cancel",
	)

	fun truthy(list: List<String>): StringBoolean {
		truthyList = list
		return this
	}

	fun falsy(list: List<String>): StringBoolean {
		falsyList = list
		return this
	}

	override fun parse(value: Any?): Boolean {
		val str = value?.toString()?.trim()?.lowercase() ?: return false
		if (str.isEmpty()) return false

		if (truthyList.any { it == str }) return true
		if (falsyList.any { it == str }) return false

		return false
	}
}