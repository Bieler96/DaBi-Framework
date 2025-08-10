package validation.schema.dsl

import validation.schema.feature.ArraySchema
import validation.schema.feature.BooleanSchema
import validation.schema.feature.NumberSchema
import validation.schema.feature.ObjectSchema
import validation.schema.feature.StringBoolean
import validation.schema.feature.StringSchema

fun string() = StringSchema()

fun string(block: StringSchema.() -> Unit): StringSchema {
	return StringSchema().apply(block)
}

fun number() = NumberSchema()

fun number(block: NumberSchema.() -> Unit): NumberSchema {
	return NumberSchema().apply(block)
}

fun boolean() = BooleanSchema()

fun boolean(block: BooleanSchema.() -> Unit): BooleanSchema {
	return BooleanSchema().apply(block)
}

fun <T> array(item: Schema<T>) = ArraySchema(item)

fun <T> array(item: Schema<T>, block: ArraySchema<T>.() -> Unit): ArraySchema<T> {
	return ArraySchema(item).apply(block)
}

fun obj(vararg pairs: Pair<String, Schema<*>>) = ObjectSchema(mapOf(*pairs))

fun obj(vararg pairs: Pair<String, Schema<*>>, block: ObjectSchema.() -> Unit): ObjectSchema {
	return ObjectSchema(mapOf(*pairs)).apply(block)
}

inline fun <reified T> validate(data: T, schema: Map<String, Schema<*>>): Map<String, Any?> {
	val dataMap = T::class.members
		.filter { it.parameters.size == 1 }
		.associateBy({ it.name }, { it.call(data) })
	return obj(*schema.toList().toTypedArray()).parse(dataMap)
}

fun stringBoolean() = StringBoolean()

fun stringBoolean(block: StringBoolean.() -> Unit): StringBoolean {
	return StringBoolean().apply(block)
}