package dbdata.query

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

class QueryMethodParser {
	fun parseMethodName(methodName: String): QueryInfo {
		val (type, queryPart) = when {
			methodName.startsWith("findBy") -> QueryType.FIND to methodName.removePrefix("findBy")
			methodName.startsWith("countBy") -> QueryType.COUNT to methodName.removePrefix("countBy")
			methodName.startsWith("deleteBy") -> QueryType.DELETE to methodName.removePrefix("deleteBy")
			methodName.startsWith("existsBy") -> QueryType.EXISTS to methodName.removePrefix("existsBy")
			else -> return QueryInfo(QueryType.CUSTOM, methodName)
		}

		val querySpec = parseQuerySpec(queryPart)
		val primaryProperty = querySpec.conditions.firstOrNull()?.property ?: ""
		val queryInfo = QueryInfo(type, primaryProperty, querySpec)

		// Debug output
		println("DEBUG: Parsed method '$methodName' -> queryInfo: $queryInfo")
		return queryInfo
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