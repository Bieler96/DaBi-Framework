package validation.schema.feature

import validation.error.TypeError
import validation.schema.dsl.Schema

class ObjectSchema(private val shape: Map<String, Schema<*>>) : Schema<Map<String, Any?>> {
	override fun parse(value: Any?): Map<String, Any?> {
		val obj = value as? Map<*, *> ?: throw TypeError("object")
		return shape.mapValues { (key, schema) ->
			schema.parse(obj[key])
		}
	}
}