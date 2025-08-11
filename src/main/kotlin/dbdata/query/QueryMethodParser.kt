package dbdata.query

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

class QueryMethodParser {
	fun parseMethodName(methodName: String): QueryInfo {
		val (type, tempMethodName, distinct) = when {
			methodName.startsWith("findDistinctBy") -> Triple(QueryType.FIND, methodName.removePrefix("findDistinctBy"), true)
			methodName.startsWith("findBy") -> Triple(QueryType.FIND, methodName.removePrefix("findBy"), false)
			methodName.startsWith("countBy") -> Triple(QueryType.COUNT, methodName.removePrefix("countBy"), false)
			methodName.startsWith("deleteBy") -> Triple(QueryType.DELETE, methodName.removePrefix("deleteBy"), false)
			methodName.startsWith("existsBy") -> Triple(QueryType.EXISTS, methodName.removePrefix("existsBy"), false)
			else -> return QueryInfo(QueryType.CUSTOM, methodName)
		}

		var rest = tempMethodName

		val limitRegex = Regex("Limit([0-9]+)$")
		var limit: Int? = null
		limitRegex.find(rest)?.let {
			limit = it.groupValues[1].toInt()
			rest = rest.substring(0, it.range.first)
		}

		val offsetRegex = Regex("Offset([0-9]+)$")
		var offset: Long? = null
		offsetRegex.find(rest)?.let {
			offset = it.groupValues[1].toLong()
			rest = rest.substring(0, it.range.first)
		}

		val orderByParts = rest.split("OrderBy")
		val queryPart = orderByParts[0]
		val sortPart = if (orderByParts.size > 1) orderByParts[1] else null

		val querySpec = if (queryPart.isNotEmpty()) parseQuerySpec(queryPart) else QuerySpec(emptyList(), LogicalOperator.AND)
		val sort = sortPart?.let { if (it.isNotEmpty()) parseSort(it) else null }

		val primaryProperty = querySpec.conditions.firstOrNull()?.property ?: ""
		val queryInfo = QueryInfo(type, primaryProperty, querySpec, sort, limit, offset, distinct)

		// Debug output
		println("DEBUG: Parsed method '$methodName' -> queryInfo: $queryInfo")
		return queryInfo
	}

	private fun parseSort(sortPart: String): Sort {
		val orders = mutableListOf<Sort.Order>()
		var remaining = sortPart
		val directionRegex = "(Asc|Desc)$".toRegex()

		while (remaining.isNotEmpty()) {
			val directionMatch = directionRegex.find(remaining)
			val direction = if (directionMatch != null && directionMatch.value == "Desc") Sort.Direction.DESC else Sort.Direction.ASC

			val propertyPart = if (directionMatch != null) remaining.removeSuffix(directionMatch.value) else remaining
			if (propertyPart.isEmpty()) break

			val propertyNameWords = propertyPart.split(Regex("(?=[A-Z])")).filter { it.isNotBlank() }
			if (propertyNameWords.isEmpty()) break

			val lastPropertyWord = propertyNameWords.last()
			orders.add(Sort.Order(extractPropertyName(lastPropertyWord), direction))

			remaining = propertyPart.removeSuffix(lastPropertyWord)
		}
		return Sort(orders.reversed())
	}

	private fun parseQuerySpec(queryPart: String): QuerySpec {
		// Handle compound queries with And/Or
		val andParts = queryPart.split("And")

		if (andParts.size > 1) {
			// Multiple conditions with AND
			val conditions = andParts.map { parseCondition(it) }
			return QuerySpec(conditions, LogicalOperator.AND)
		}

		val orParts = queryPart.split("Or")
		if (orParts.size > 1) {
			// Multiple conditions with OR
			val conditions = orParts.map { parseCondition(it) }
			return QuerySpec(conditions, LogicalOperator.OR)
		}

		// Single condition
		val condition = parseCondition(queryPart)
		return QuerySpec(listOf(condition), LogicalOperator.AND)
	}

	private fun parseCondition(conditionPart: String): QueryCondition {
		return when {
			conditionPart.endsWith("GreaterThan") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("GreaterThan"))
				QueryCondition(property, QueryOperator.GREATER_THAN)
			}

			conditionPart.endsWith("LessThan") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("LessThan"))
				QueryCondition(property, QueryOperator.LESS_THAN)
			}

			conditionPart.endsWith("GreaterThanEqual") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("GreaterThanEqual"))
				QueryCondition(property, QueryOperator.GREATER_THAN_EQUAL)
			}

			conditionPart.endsWith("LessThanEqual") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("LessThanEqual"))
				QueryCondition(property, QueryOperator.LESS_THAN_EQUAL)
			}

			conditionPart.endsWith("Containing") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("Containing"))
				QueryCondition(property, QueryOperator.CONTAINING)
			}

			conditionPart.endsWith("ContainingIgnoreCase") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("ContainingIgnoreCase"))
				QueryCondition(property, QueryOperator.CONTAINING_IGNORE_CASE)
			}

			conditionPart.endsWith("StartingWith") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("StartingWith"))
				QueryCondition(property, QueryOperator.STARTING_WITH)
			}

			conditionPart.endsWith("EndingWith") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("EndingWith"))
				QueryCondition(property, QueryOperator.ENDING_WITH)
			}

			conditionPart.endsWith("IsNull") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("IsNull"))
				QueryCondition(property, QueryOperator.IS_NULL)
			}

			conditionPart.endsWith("IsNotNull") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("IsNotNull"))
				QueryCondition(property, QueryOperator.IS_NOT_NULL)
			}

			conditionPart.endsWith("In") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("In"))
				QueryCondition(property, QueryOperator.IN)
			}

			conditionPart.endsWith("NotIn") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("NotIn"))
				QueryCondition(property, QueryOperator.NOT_IN)
			}

			conditionPart.endsWith("Between") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("Between"))
				QueryCondition(property, QueryOperator.BETWEEN)
			}

			conditionPart.endsWith("Not") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("Not"))
				QueryCondition(property, QueryOperator.NOT)
			}

			conditionPart.endsWith("IsEmpty") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("IsEmpty"))
				QueryCondition(property, QueryOperator.IS_EMPTY)
			}

			conditionPart.endsWith("IsNotEmpty") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("IsNotEmpty"))
				QueryCondition(property, QueryOperator.IS_NOT_EMPTY)
			}

			conditionPart.endsWith("True") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("True"))
				QueryCondition(property, QueryOperator.TRUE)
			}

			conditionPart.endsWith("False") -> {
				val property = extractPropertyName(conditionPart.removeSuffix("False"))
				QueryCondition(property, QueryOperator.FALSE)
			}

			else -> {
				// Simple equals condition
				val property = extractPropertyName(conditionPart)
				QueryCondition(property, QueryOperator.EQUALS)
			}
		}
	}

	private fun extractPropertyName(suffix: String): String {
		// Convert CamelCase to lowercase and handle nested properties (e.g., UserName -> user.name, User_Name -> user.name)
		val result = StringBuilder()
		suffix.forEachIndexed { index, char ->
			if (index == 0) {
				result.append(char.lowercase())
			} else if (char.isUpperCase()) {
				result.append(".").append(char.lowercase())
			} else {
				result.append(char)
			}
		}
		return result.toString().replace("_", ".")
	}

	fun shouldReturnSingle(methodName: String, returnType: KType?): Boolean {
		if (returnType == null) return false // Or handle as an error

		// Check if the return type is a List or a Collection
		val classifier = returnType.classifier
		return !(classifier is KClass<*> && (classifier.isSubclassOf(List::class) || classifier.isSubclassOf(Collection::class)))
	}
}