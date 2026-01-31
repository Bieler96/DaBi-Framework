package dbdata.dataprovider

import dbdata.*
import dbdata.exception.EntityNotFoundException
import dbdata.query.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
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
		buildPropertyToColumnMapping(table)
	}

	override fun getEntityClass(): KClass<T> = entityClass

	override suspend fun save(entity: T): T = transaction(db = database) {
		val now = Instant.now()
		// TODO: Replace with actual user from context.
		// This is a placeholder that infers the user ID type from the entity's ID type,
		// which might not be correct in all scenarios.
		val currentUser: Long? = null

		if (entity.id == null) {
			// Insert new entity
			val insertStatement = table.insert {
				(entity as Entity<*>).createdAt = now
				(entity as Entity<*>).updatedAt = now
				fillStatementFromEntity(it, entity, excludeId = true)
			}
			// Return entity with generated ID and populated audit fields
			val generatedId = insertStatement[idColumn]
			setEntityId(entity, generatedId)
		} else {
			// Update existing entity
			(entity as Entity<*>).updatedAt = now
			val updatedRows = table.update({ idColumn eq entity.id!! }) {
				fillStatementFromEntity(it, entity, excludeId = true)
			}
			if (updatedRows == 0) {
				throw EntityNotFoundException(entityClass.simpleName ?: "Entity", entity.id!!)
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

	override suspend fun findById(id: ID): T? = transaction(db = database) {
		findWithRelations(idColumn eq id).firstOrNull()
	}

	override suspend fun findAll(): List<T> = transaction(db = database) {
		findWithRelations(null)
	}

	override suspend fun findAll(pageable: Pageable): List<T> = transaction(db = database) {
		findWithRelations(null, pageable.sort, pageable.pageSize, pageable.offset)
	}

	override suspend fun findAll(sort: Sort): List<T> = transaction(db = database) {
		val query = table.selectAll()
		applySort(query, sort).mapNotNull { mapRowToEntity(it, table, entityClass) }
	}

	override suspend fun deleteById(id: ID): Long = transaction(db = database) {
		table.deleteWhere { idColumn eq id }.toLong()
	}

	override suspend fun delete(entity: T): Long = transaction(db = database) {
		if (entity.id != null) {
			table.deleteWhere { idColumn eq entity.id!! }.toLong()
		} else {
			0L
		}
	}

	override suspend fun deleteAllInBatch(entities: Iterable<T>): Long = transaction(db = database) {
		val ids = entities.mapNotNull { it.id }
		if (ids.isNotEmpty()) {
			table.deleteWhere { idColumn inList ids }.toLong()
		} else {
			0L
		}
	}

	override suspend fun deleteAll(): Long = transaction(db = database) {
		table.deleteAll().toLong()
	}

	override suspend fun count(): Long = transaction(db = database) {
		table.selectAll().count()
	}

	@Suppress("UNCHECKED_CAST")
	override suspend fun findByProperty(property: String, value: Any): List<T> =
		transaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.selectAll().where { column eq value }.mapNotNull { mapRowToEntity(it, table, entityClass) }
		}

	@Suppress("UNCHECKED_CAST")
	override suspend fun countByProperty(property: String, value: Any): Long =
		transaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.selectAll().where { column eq value }.count()
		}

	@Suppress("UNCHECKED_CAST")
	override suspend fun deleteByProperty(property: String, value: Any): Long =
		transaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.deleteWhere { column eq value }.toLong()
		}

	@Suppress("UNCHECKED_CAST")
	override suspend fun existsByProperty(property: String, value: Any): Boolean =
		transaction(db = database) {
			val column = getColumnByName(property, propertyToColumnMap) as Column<Any>
			table.selectAll().where { column eq value }.limit(1).count() > 0
		}

	override suspend fun executeCustomQuery(query: String, params: Map<String, Any>): List<T> =
		transaction(db = database) {
			val results = mutableListOf<T>()
			var processedQuery = query

			// Manually substitute parameters into the query string
			// WARNING: This approach is vulnerable to SQL injection if parameter values are not properly sanitized.
			// For production code, use prepared statements with proper parameter binding.
			params.forEach { (key, value) ->
				processedQuery = processedQuery.replace(":$key", value.toString())
			}

			exec(processedQuery) { rs ->
				while (rs.next()) {
					val valuesByColumn: Map<Column<*>, Any?> =
						table.columns.associateWith { col -> rs.getObject(col.name) }
					val exprValues: Map<org.jetbrains.exposed.v1.core.Expression<*>, Any?> =
						valuesByColumn.mapKeys { (col, _) -> col as org.jetbrains.exposed.v1.core.Expression<*> }
					val row = ResultRow.createAndFillValues(exprValues)
					mapRowToEntity(row, table, entityClass)?.let { results.add(it) }
				}
			}
			results
		}

	// ============= NEW ADVANCED QUERY METHODS =============

	override suspend fun findByQuerySpec(
		querySpec: QuerySpec,
		parameters: List<Any>,
		sort: Sort?,
		limit: Int?,
		offset: Long?,
		distinct: Boolean,
		projectionClass: KClass<*>?
	): List<Any> =
		transaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(querySpec, parameters)
			val query = currentSource.selectAll().where { whereClause }.withDistinct(distinct)

			sort?.let {
				applySort(query, it)
			}

			limit?.let {
				query.limit(it).offset(offset ?: 0L)
			}

			query.mapNotNull { mapRowToEntity(it, table, entityClass) }
		}

	override suspend fun countByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long =
		transaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(querySpec, parameters)
			currentSource.selectAll().where { whereClause }.count()
		}

	override suspend fun deleteByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long =
		transaction(db = database) {
			val (whereClause, _) = buildWhereClause(querySpec, parameters)
			table.deleteWhere { whereClause }.toLong()
		}

	override suspend fun existsByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Boolean =
		transaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(querySpec, parameters)
			currentSource.selectAll().where { whereClause }.limit(1).count() > 0
		}

	override suspend fun aggregate(queryInfo: QueryInfo, parameters: List<Any>): Any? =
		transaction(db = database) {
			val (whereClause, currentSource) = buildWhereClause(queryInfo.querySpec, parameters)

			if (queryInfo.groupByProperty != null) {
				val groupByColumn = getColumnByName(queryInfo.groupByProperty!!, propertyToColumnMap)
					?: throw IllegalArgumentException("Column for groupBy property '${"$"}{queryInfo.groupByProperty!!}' not found.")

				val countColumn = if (queryInfo.property == "*") idColumn else getColumnByName(
					queryInfo.property,
					propertyToColumnMap
				)!!
				val countExpression = Count(countColumn)

				val query = currentSource
					.select(groupByColumn, countExpression)
					.where { whereClause }
					.groupBy(groupByColumn)

				query.associate { row ->
					row[groupByColumn] to row[countExpression]
				}

			} else {
				val propertyToAggregate = queryInfo.property
				val columnToAggregate = getColumnByName(propertyToAggregate, propertyToColumnMap)
					?: throw IllegalArgumentException("Column for property '$propertyToAggregate' not found.")
				val aggregationFunction =
					queryInfo.aggregationFunction ?: throw IllegalStateException("Aggregation function is missing")

				val expression: Expression<*> = when (aggregationFunction) {
					AggregationFunction.SUM -> {
						@Suppress("UNCHECKED_CAST")
						when (val columnType = columnToAggregate.columnType) {
							is IntegerColumnType -> Sum(columnToAggregate as Column<Int?>, columnType)
							is LongColumnType -> Sum(columnToAggregate as Column<Long?>, columnType)
							is FloatColumnType -> Sum(columnToAggregate as Column<Float?>, columnType)
							is DoubleColumnType -> Sum(columnToAggregate as Column<Double?>, columnType)
							is DecimalColumnType -> Sum(columnToAggregate as Column<BigDecimal?>, columnType)
							else -> throw IllegalArgumentException("Unsupported column type for SUM: ${"$"}{columnType::class.simpleName}")
						}
					}

					AggregationFunction.AVG -> {
						@Suppress("UNCHECKED_CAST")
						when (columnToAggregate.columnType) {
							is IntegerColumnType -> Avg(columnToAggregate as Column<Int?>, 2)
							is LongColumnType -> Avg(columnToAggregate as Column<Long?>, 2)
							is FloatColumnType -> Avg(columnToAggregate as Column<Float?>, 2)
							is DoubleColumnType -> Avg(columnToAggregate as Column<Double?>, 2)
							is DecimalColumnType -> Avg(columnToAggregate as Column<BigDecimal?>, 2)
							else -> throw IllegalArgumentException("Unsupported column type for AVG: ${"$"}{columnToAggregate.columnType::class.simpleName}")
						}
					}

					AggregationFunction.MIN -> Min(
						columnToAggregate as Column<out Comparable<Any>?>,
						columnToAggregate.columnType
					)

					AggregationFunction.MAX -> Max(
						columnToAggregate as Column<out Comparable<Any>?>,
						columnToAggregate.columnType
					)

					AggregationFunction.COUNT -> Count(columnToAggregate)
				}

				val query = currentSource.select(expression).where { whereClause }
				val result = query.firstOrNull()?.get(expression)

				result
			}
		}

	// ============= PRIVATE HELPER METHODS =============

	private fun findWithRelations(
		where: Op<Boolean>?,
		sort: Sort? = null,
		limit: Int? = null,
		offset: Long? = null
	): List<T> {
		val eagerRelations = entityClass.memberProperties.filter { prop ->
			prop.annotations.any { it is OneToMany || it is ManyToOne } &&
					(prop.findAnnotation<OneToMany>()?.fetch == FetchType.EAGER || prop.findAnnotation<ManyToOne>()?.fetch == FetchType.EAGER)
		}

		if (eagerRelations.isEmpty()) {
			val query = table.selectAll()
			where?.let { query.where(it) }
			sort?.let { applySort(query, it) }
			limit?.let { query.limit(it).offset(offset ?: 0L) }
			return query.mapNotNull { mapRowToEntity(it, table, entityClass) }
		}

		var join: ColumnSet = table
		val relationData = eagerRelations.mapNotNull { prop ->
			val relationAnnotation =
				prop.annotations.firstOrNull { it is ManyToOne || it is OneToMany } ?: return@mapNotNull null
			val targetEntityClass = prop.returnType.arguments.firstOrNull()?.type?.classifier as? KClass<Entity<*>>
				?: prop.returnType.classifier as? KClass<Entity<*>> ?: return@mapNotNull null

			val targetTable =
				allTables.find { it.tableName.equals(targetEntityClass.simpleName!! + "s", ignoreCase = true) }
					?: return@mapNotNull null

			when (relationAnnotation) {
				is ManyToOne -> {
					val joinColumnAnn = prop.findAnnotation<JoinColumn>() ?: return@mapNotNull null
					val fkColumn = table.columns.find { it.name == joinColumnAnn.name } ?: return@mapNotNull null
					val pkColumn = targetTable.columns.find { it.name == joinColumnAnn.referencedColumnName }
						?: return@mapNotNull null
					join = (join as Table).join(targetTable, JoinType.LEFT, onColumn = fkColumn, otherColumn = pkColumn)
					Triple(prop, targetEntityClass, targetTable)
				}

				is OneToMany -> {
					val mappedBy = relationAnnotation.mappedBy
					val targetProp =
						targetEntityClass.memberProperties.find { it.name == mappedBy } ?: return@mapNotNull null
					val joinColumnAnn = targetProp.findAnnotation<JoinColumn>() ?: return@mapNotNull null
					val fkColumn = targetTable.columns.find { it.name == joinColumnAnn.name } ?: return@mapNotNull null
					val pkColumn =
						table.columns.find { it.name == joinColumnAnn.referencedColumnName } ?: return@mapNotNull null
					join = (join as Table).join(targetTable, JoinType.LEFT, onColumn = pkColumn, otherColumn = fkColumn)
					Triple(prop, targetEntityClass, targetTable)
				}

				else -> null
			}
		}

		val query = join.selectAll()
		where?.let { query.where(it) }
		sort?.let { applySort(query, it) }
		limit?.let { query.limit(it).offset(offset ?: 0L) }

		val results = query.toList()
		if (results.isEmpty()) return emptyList()

		val mainEntities = mutableMapOf<ID, T>()

		results.forEach { row ->
			val mainId = row[idColumn]
			val mainEntity = mainEntities.getOrPut(mainId) { mapRowToEntity(row, table, entityClass)!! }

			relationData.forEach { (prop, targetClass, targetTable) ->
				when {
					prop.findAnnotation<ManyToOne>() != null -> {
						val relatedEntity = mapRowToEntity(row, targetTable, targetClass)
						(prop as KMutableProperty1<T, Any?>).set(mainEntity, relatedEntity)
					}

					prop.findAnnotation<OneToMany>() != null -> {
						val relatedEntity = mapRowToEntity(row, targetTable, targetClass)
						if (relatedEntity != null) {
							@Suppress("UNCHECKED_CAST")
							val list = (prop.get(mainEntity) as? MutableList<Entity<*>> ?: mutableListOf())
							if (!list.contains(relatedEntity)) {
								list.add(relatedEntity)
							}
							(prop as KMutableProperty1<T, Any?>).set(mainEntity, list)
						}
					}
				}
			}
		}
		return mainEntities.values.toList()
	}

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
						?: throw IllegalStateException("No primary key found for ${"$"}{foundRelationTable.tableName}")

					// Find FK in the current source table that references the other table's PK.
					val foreignKeyColumn = currentTableSource.columns.find { it.referee == otherTablePk }
					// Fallback to naming convention if no FK reference is defined.
						?: currentTableSource.columns.find {
							it.name.equals(
								"${"$"}{relationName}Id",
								ignoreCase = true
							)
						}
						?: throw IllegalArgumentException("Foreign key for '$relationName' not found in source tables. Looked for a column referencing ${"$"}{foundRelationTable.tableName}'s PK and a column named '${"$"}{relationName}Id'.")

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
				column = getColumnByName(propertyName, buildPropertyToColumnMapping(targetTable))!!

			} else {
				targetTable = table
				column = getColumnByName(condition.property, propertyToColumnMap)!!
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
					(column as Column<String>) like "%${"$"}{value}%"
				}

				QueryOperator.CONTAINING_IGNORE_CASE -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "%${"$"}{value}%"
				}

				QueryOperator.STARTING_WITH -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "${"$"}{value}%"
				}

				QueryOperator.ENDING_WITH -> {
					val value = parameters[parameterIndex++].toString()
					(column as Column<String>) like "%${"$"}{value}"
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

				QueryOperator.NOT -> {
					val value = parameters[parameterIndex++]
					(column as Column<Any>) neq value
				}

				QueryOperator.IS_EMPTY -> {
					(column as Column<String>) eq ""
				}

				QueryOperator.IS_NOT_EMPTY -> {
					(column as Column<String>) neq ""
				}

				QueryOperator.TRUE -> {
					(column as Column<Boolean>) eq true
				}

				QueryOperator.FALSE -> {
					(column as Column<Boolean>) eq false
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

	private fun <E : Entity<*>> mapRowToEntity(row: ResultRow, table: Table, entityClass: KClass<E>): E? {
		val constructor = entityClass.primaryConstructor
			?: throw IllegalArgumentException("Entity ${"$"}{entityClass.simpleName} must have a primary constructor")

		val idColumn = table.primaryKey?.columns?.firstOrNull() ?: table.columns.firstOrNull {
			it.name.equals(
				"id",
				ignoreCase = true
			)
		} ?: return null

		if (!row.fieldIndex.containsKey(idColumn) || row[idColumn] == null) {
			return null
		}

		val propertyToColumnMap = buildPropertyToColumnMapping(table)

		val args: Map<KParameter, Any?> = constructor.parameters.associateWith { param ->
			val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")

			when (paramName) {
				"id" -> row[idColumn]
				"createdAt", "updatedAt" -> getColumnByName(paramName, propertyToColumnMap)?.let { row[it] }?.let {
					if (it is Long) Instant.ofEpochSecond(it) else it
				}

				else -> {
					val column = getColumnByName(paramName, propertyToColumnMap, true)
					if (column != null && row.fieldIndex.containsKey(column)) {
						row[column]
					} else {
						null
					}
				}
			}
		}

		return try {
			val nonNullArgs = args.filterValues { it != null }

			@Suppress("UNCHECKED_CAST")
			val entity = constructor.callBy(nonNullArgs as Map<KParameter, Any>)

			// Manually set nullable properties that were not in the constructor call
			entityClass.memberProperties.forEach { prop ->
				val param = constructor.parameters.find { it.name == prop.name }
				if (prop is KMutableProperty1<E, *> && param != null && args[param] == null) {
					if (prop.returnType.isMarkedNullable) {
						(prop as KMutableProperty1<E, Any?>).set(entity, null)
					}
				}
			}

			entity
		} catch (e: IllegalArgumentException) {
			// This can happen if a non-nullable constructor argument is null in the 'args' map.
			// With the id check, this should indicate inconsistent data rather than a join issue.
			// For now, we return null to avoid crashing. Consider logging this event.
			null
		}
	}

	private fun getColumnByName(
		name: String,
		columnMap: Map<String, Column<*>>,
		optional: Boolean = false
	): Column<*>? {
		val column = columnMap[name]
		if (column == null && !optional) {
			throw IllegalArgumentException("No column found for property: $name")
		}
		return column
	}

	private fun buildPropertyToColumnMapping(table: Table): Map<String, Column<*>> {
		// This new implementation assumes entity property names are camelCase versions of snake_case or uppercase column names.
		fun toCamelCase(snake: String): String {
			return snake.split('_').mapIndexed { index, s ->
				if (index == 0) s.lowercase() else s.lowercase().replaceFirstChar { it.uppercase() }
			}.joinToString("")
		}

		return table.columns.associateBy { toCamelCase(it.name) }
	}

	private fun fillStatementFromEntity(statement: UpdateBuilder<*>, entity: T, excludeId: Boolean = false) {
		val isInsert = entity.id == null

		entityClass.memberProperties.forEach { prop ->
			if (excludeId && prop.name == "id") return@forEach

			// Do not update createdAt/By on update statements
			if (!isInsert && (prop.name == "createdAt" || prop.name == "createdBy")) {
				return@forEach
			}

			val column = propertyToColumnMap[prop.name]
			if (column != null) {
				prop.isAccessible = true
				val value = prop.get(entity)

				if (value != null) {
					(column as? Column<Any>)?.let { statement[it] = value }
				}
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

	private fun applySort(query: Query, sort: Sort): Query {
		sort.orders.forEach { order ->
			val column = getColumnByName(order.property, propertyToColumnMap)
			query.orderBy(
				column!!,
				if (order.direction == Sort.Direction.ASC) org.jetbrains.exposed.v1.core.SortOrder.ASC else org.jetbrains.exposed.v1.core.SortOrder.DESC
			)
		}
		return query
	}
}