package dbdata.query

data class QuerySpec(
	val conditions: List<QueryCondition>,
	val logicalOperator: LogicalOperator
)