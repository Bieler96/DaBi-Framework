package validation.schema.core

interface Schema<T> {
	fun parse(value: Any?): T?
}