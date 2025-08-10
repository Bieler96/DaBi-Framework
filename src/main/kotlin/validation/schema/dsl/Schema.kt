package validation.schema.dsl

interface Schema<T> {
	fun parse(value: Any?): T?
}