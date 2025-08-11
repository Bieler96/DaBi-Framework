package dbdata

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Repository

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Query(val value: String = "")

enum class FetchType {
	LAZY, EAGER
}

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneToOne(
	val mappedBy: String = "",
	val fetch: FetchType = FetchType.EAGER
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneToMany(
	val mappedBy: String = "",
	val fetch: FetchType = FetchType.LAZY
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ManyToOne(
	val fetch: FetchType = FetchType.EAGER
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ManyToMany(
	val fetch: FetchType = FetchType.LAZY
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class JoinColumn(
	val name: String,
	val referencedColumnName: String = "id"
)