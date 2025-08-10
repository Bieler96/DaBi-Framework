package dbdata.dataprovider

import dbdata.Entity
import dbdata.Auditable
import dbdata.query.LogicalOperator
import dbdata.query.QueryOperator
import dbdata.query.QuerySpec
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnSet
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.between
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greater
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.like
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class ExposedDataProvider<T : Entity<ID>, ID>(
	private val table: Table,
	private val entityClass: KClass<T>,
	private val idColumn: Column<ID>,
	private val database: Database,
	private val allTables: List<Table> // Added parameter
) : DataProvider<T, ID>() {

	// Cache für Performance
	private val propertyToColumnMap: Map<String, Column<*>> by lazy {
		buildPropertyToColumnMapping()
	}

	override suspend fun save(entity: T): T = newSuspendedTransaction(db = database) {
		val now = LocalDateTime.now()
		val currentUser = "system" // TODO: Replace with actual user from context

		if (entity.id == null) {
			// Insert new entity
			val insertStatement = table.insert {
				(entity as Auditable).createdAt = now
				(entity as Auditable).updatedAt = now
				(entity as Auditable).createdBy = currentUser
				(entity as Auditable).updatedBy = currentUser
				fillStatementFromEntity(it, entity, excludeId = true)
			}
			// Return entity with generated ID and populated audit fields
			val generatedId = insertStatement[idColumn]
			setEntityId(entity, generatedId)
		} else {
			// Update existing entity
			(entity as Auditable).updatedAt = now
			(entity as Auditable).updatedBy = currentUser
			table.update({ idColumn eq entity.id!! }) {
				fillStatementFromEntity(it, entity, excludeId = true)
			}
			entity
		}
	}

	override suspend fun saveAll(entities: Iterable<T>): List<T> {
		val saved = mutableListOf<T>()
		for (entity in entities) {
			saved.add(save(entity))
		}
		return saved
	}

	override suspend fun findById(id: ID): T? = newSuspendedTransaction(db = database) {
		table.selectAll().where { idColumn eq id }
			.map { mapRowToEntity(it) }
			.singleOrNull()
	}

	override suspend fun findAll(): List<T> = newSuspendedTransaction(db = database) {
		table.selectAll().map { mapRowToEntity(it) }
	}

	override suspend fun deleteById(id: ID): Long = newSuspendedTransaction(db = database) {
		table.deleteWhere { idColumn eq id }.toLong()
	}

	override suspend fun delete(entity: T): Long = newSuspendedTransaction(db = database) {
		if (entity.id != null) {
			table.deleteWhere { idColumn eq entity.id!! }.toLong()
		} else {
			0L
		}
	}

	override suspend fun deleteAllInBatch(entities: Iterable<T>): Long = newSuspendedTransaction(db = database) {
		val ids = entities.mapNotNull { it.id }
		if (ids.isNotEmpty()) {
			table.deleteWhere { idColumn inList ids }.toLong()
		} else {
			0L
		}
	}

	override suspend fun deleteAll(): Long = newSuspendedTransaction(db = database) {
		table.deleteAll().toLong()
	}

	override suspend fun count(): Long = newSuspendedTransaction(db = database) {
		table.selectAll().count()
	}

	@Suppress("UNCHECKED_CAST")
	override suspend fun findByProperty(property: String, value: Any): List<T> =
		newSuspendedTransaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.selectAll().where { column eq value }.map { mapRowToEntity(it) }
		}

	@Suppress("UNCHECKED_CAST")
	override suspend fun countByProperty(property: String, value: Any): Long =
		newSuspendedTransaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.selectAll().where { column eq value }.count()
		}

	@Suppress("UNCHECKED_CAST")
	override suspend fun deleteByProperty(property: String, value: Any): Long =
		newSuspendedTransaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.deleteWhere { column eq value }.toLong()
		}

	@Suppress("UNCHECKED_CAST")
	override suspend fun existsByProperty(property: String, value: Any): Boolean =
		newSuspendedTransaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.selectAll().where { column eq value }.limit(1).count() > 0
		}

	override suspend fun executeCustomQuery(query: String, params: Map<String, Any>): List<T> =
		newSuspendedTransaction(db = database) {
			// Custom SQL execution would require additional SQL parsing and execution
			// This is a placeholder for custom raw SQL queries
			emptyList()
		}

	// ============= NEW ADVANCED QUERY METHODS =============

	override suspend fun findByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): List<T> =
		newSuspendedTransaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(querySpec, parameters)
			currentSource.selectAll().where { whereClause }.map { mapRowToEntity(it) }
		}

	override suspend fun countByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long =
		newSuspendedTransaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(querySpec, parameters)
			currentSource.selectAll().where { whereClause }.count()
		}

	override suspend fun deleteByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long =
		newSuspendedTransaction(db = database) {
			val (whereClause, _) = buildWhereClause(querySpec, parameters)
			table.deleteWhere { whereClause }.toLong()
		}

	override suspend fun existsByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Boolean =
		newSuspendedTransaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(querySpec, parameters)
			currentSource.selectAll().where { whereClause }.limit(1).count() > 0
		}

	// ============= PRIVATE HELPER METHODS =============

	@Suppress("UNCHECKED_CAST")
	private fun buildWhereClause(querySpec: QuerySpec, parameters: List<Any>): Pair<Op<Boolean>, ColumnSet> {
		if (querySpec.conditions.isEmpty()) {
			return Pair(Op.TRUE, table)
		}

		var parameterIndex = 0
		var currentTableSource: ColumnSet = table
		val joinedTables = mutableSetOf<Table>() // To avoid joining the same table multiple times

		val conditions = querySpec.conditions.map { condition ->
			val propertyParts = condition.property.split(".")
			val column: Column<*>
			val targetTable: Table

			if (propertyParts.size > 1) {
				val relationName = propertyParts[0] // e.g., "user"
				val propertyName = propertyParts.drop(1).joinToString(".") // e.g., "name" or "address.city"

				// Convention: 'user' -> find 'users' table
				val foundRelationTable = allTables.find { it.tableName.equals(relationName + "s", ignoreCase = true) }
					?: throw IllegalArgumentException("Related table for '$relationName' not found.")

				if (foundRelationTable !in joinedTables) {
					// Find PK of the other table.
					val otherTablePk = foundRelationTable.primaryKey?.columns?.firstOrNull()
						?: foundRelationTable.columns.firstOrNull { it.name.equals("id", ignoreCase = true) }
						?: throw IllegalStateException("No primary key found for ${foundRelationTable.tableName}")

					// Find FK in the current source table that references the other table's PK.
					val foreignKeyColumn = currentTableSource.columns.find { it.referee == otherTablePk }
						// Fallback to naming convention if no FK reference is defined.
						?: currentTableSource.columns.find { it.name.equals("${relationName}Id", ignoreCase = true) }
						?: throw IllegalArgumentException("Foreign key for '$relationName' not found in source tables. Looked for a column referencing ${foundRelationTable.tableName}'s PK and a column named '${relationName}Id'.")

					currentTableSource = when (val current = currentTableSource) {
						is Table -> current.join(
							foundRelationTable,
							JoinType.INNER,
							onColumn = foreignKeyColumn,
							otherColumn = otherTablePk
						)
						is Join -> current.join(
							foundRelationTable,
							JoinType.INNER,
							onColumn = foreignKeyColumn,
							otherColumn = otherTablePk
						)
						else -> error("Unsupported ColumnSet type for join: $current")
					}
					joinedTables.add(foundRelationTable)
				}
				targetTable = foundRelationTable
				column = getColumnByName(propertyName, targetTable.columns.associateBy { it.name })

			} else {
				targetTable = table
				column = getColumnByName(condition.property, propertyToColumnMap)
			}

			val expr = when (condition.operator) {
				QueryOperator.EQUALS -> {
					val value = parameters[parameterIndex++]
					(column as Column<Any>) eq value
				}
				QueryOperator.GREATER_THAN -> {
					val value = parameters[parameterIndex++] as Comparable<Any>
					(column as Column<Comparable<Any>>) greater value
				}
				QueryOperator.LESS_THAN -> {
					val value = parameters[parameterIndex++] as Comparable<Any>
					(column as Column<Comparable<Any>>) less value
				}
				QueryOperator.GREATER_THAN_EQUAL -> {
					val value = parameters[parameterIndex++] as Comparable<Any>
					(column as Column<Comparable<Any>>) greaterEq value
				}
				QueryOperator.LESS_THAN_EQUAL -> {
					val value = parameters[parameterIndex++] as Comparable<Any>
					(column as Column<Comparable<Any>>) lessEq value
				}
				QueryOperator.CONTAINING -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "%${value}%"
				}
				QueryOperator.CONTAINING_IGNORE_CASE -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "%${value}%"
				}
				QueryOperator.STARTING_WITH -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "${value}%"
				}
				QueryOperator.ENDING_WITH -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "%${value}"
				}
				QueryOperator.IS_NULL -> {
					(column as Column<Any?>).isNull()
				}
				QueryOperator.IS_NOT_NULL -> {
					(column as Column<Any?>).isNotNull()
				}
				QueryOperator.IN -> {
					val values = parameters[parameterIndex++] as List<Any>
					(column as Column<Any>) inList values
				}
				QueryOperator.NOT_IN -> {
					val values = parameters[parameterIndex++] as List<Any>
					(column as Column<Any>) notInList values
				}
				QueryOperator.BETWEEN -> {
					val minValue = parameters[parameterIndex++] as Comparable<Any>
					val maxValue = parameters[parameterIndex++] as Comparable<Any>
					(column as Column<Comparable<Any>>).between(minValue, maxValue)
				}
			}
			expr
		}

		val finalOp = when (querySpec.logicalOperator) {
			LogicalOperator.AND -> conditions.reduce { acc, condition -> acc and condition }
			LogicalOperator.OR -> conditions.reduce { acc, condition -> acc or condition }
		}
		return Pair(finalOp, currentTableSource)
	}

	private fun mapRowToEntity(row: ResultRow): T {
		val constructor = entityClass.primaryConstructor
			?: throw IllegalArgumentException("Entity ${entityClass.simpleName} must have a primary constructor")

		val args = constructor.parameters.associateWith { param ->
			val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")

			when (paramName) {
				"id" -> row[idColumn]
				"createdAt", "updatedAt" -> row[getColumnByName(paramName, propertyToColumnMap)].let { 
					if (it is Long) LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) else it
				}
				else -> {
					val column = getColumnByName(paramName, propertyToColumnMap)
					row[column]
				}
			}
		}

		return constructor.callBy(args)
	}

	private fun getColumnByName(name: String, columnMap: Map<String, Column<*>>): Column<*> {
		return columnMap[name]
			?: throw IllegalArgumentException("No column found for property: $name")
	}

	private fun buildPropertyToColumnMapping(): Map<String, Column<*>> {
		val mapping = mutableMapOf<String, Column<*>>()

		val tableClass = table.javaClass.kotlin
		val columns = tableClass.memberProperties
			.filter { it.returnType.classifier == Column::class }
			.mapNotNull { prop ->
				prop.isAccessible = true
				val column = prop.get(table) as? Column<*>
				val propertyName = prop.name
				column?.let { propertyName to it }
			}

		mapping.putAll(columns)
		return mapping
	}

	private fun fillStatementFromEntity(statement: UpdateBuilder<*>, entity: T, excludeId: Boolean = false) {
		val now = LocalDateTime.now()
		val currentUser = "system" // TODO: Replace with actual user from context

		val isInsert = entity.id == null

		entityClass.memberProperties.forEach { prop ->
			if (excludeId && prop.name == "id") return@forEach

			val column = propertyToColumnMap[prop.name] ?: return@forEach
			prop.isAccessible = true
			val value = prop.get(entity)

			when (prop.name) {
				"createdAt" -> if (isInsert) statement[column as Column<Long>] = now.toEpochSecond(ZoneOffset.UTC) else return@forEach
				"updatedAt" -> statement[column as Column<Long>] = now.toEpochSecond(ZoneOffset.UTC)
				"createdBy" -> if (isInsert) statement[column as Column<String>] = currentUser else return@forEach
				"updatedBy" -> statement[column as Column<String>] = currentUser
				else -> statement[column as Column<Any?>] = value
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun setEntityId(entity: T, id: ID): T {
		val constructor = entityClass.primaryConstructor
			?: throw IllegalArgumentException("Entity must have primary constructor")

		val currentValues = entityClass.memberProperties.associate { prop ->
			prop.isAccessible = true
			prop.name to if (prop.name == "id") id else prop.get(entity)
		}

		val args = constructor.parameters.associateWith { param ->
			currentValues[param.name]
		}

		return constructor.callBy(args)
	}
}
