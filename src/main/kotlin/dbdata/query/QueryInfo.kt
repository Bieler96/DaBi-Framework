package dbdata.query

data class QueryInfo(
	val type: QueryType,
	val property: String,
	val querySpec: QuerySpec = QuerySpec(emptyList(), LogicalOperator.AND),
	val sort: Sort? = null,
	val limit: Int? = null,
	val offset: Long? = null,
	val distinct: Boolean = false
)