package dbdata.query

data class QueryInfo(
	val type: QueryType,
	val property: String,
	val querySpec: QuerySpec = QuerySpec(emptyList(), LogicalOperator.AND)
)