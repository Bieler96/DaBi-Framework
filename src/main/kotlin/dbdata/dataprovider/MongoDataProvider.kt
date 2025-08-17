package dbdata.dataprovider

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoCollection
import dbdata.Entity
import dbdata.exception.EntityNotFoundException
import dbdata.Auditable
import dbdata.query.*
import dbdata.query.AggregationFunction
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import java.time.LocalDateTime
import org.bson.Document
import org.bson.conversions.Bson
import kotlin.reflect.KClass

class MongoDataProvider<T : Entity<String>>(
    private val collection: MongoCollection<T>,
    private val entityClass: KClass<T>
) : DataProvider<T, String>() {

    override fun getEntityClass(): KClass<T> = entityClass

    override suspend fun save(entity: T): T {
        val now = LocalDateTime.now()
        val currentUser = "system" // TODO: Replace with actual user from context

        if (entity.id == null) {
            // Insert new entity
            (entity as Auditable).createdAt = now
            (entity as Auditable).updatedAt = now
            (entity as Auditable).createdBy = currentUser
            (entity as Auditable).updatedBy = currentUser
            collection.insertOne(entity)
            return entity
        } else {
            // Update existing
            (entity as Auditable).updatedAt = now
            (entity as Auditable).updatedBy = currentUser
            val result = collection.replaceOne(Filters.eq("_id", entity.id!!), entity)
            if (result.matchedCount == 0L) {
                throw EntityNotFoundException(entityClass.simpleName ?: "Entity", entity.id!!)
            }
            return entity
        }
    }

    override suspend fun saveAll(entities: Iterable<T>): List<T> {
        val saved = mutableListOf<T>()
        for (entity in entities) {
            saved.add(save(entity))
        }
        return saved
    }

    override suspend fun findById(id: String): T? {
        return collection.find(Filters.eq("_id", id)).firstOrNull()
    }

    override suspend fun findAll(): List<T> {
        return collection.find().toList()
    }

    override suspend fun findAll(pageable: Pageable): List<T> {
        var findFlow = collection.find()
        pageable.sort?.let { sort ->
            findFlow = applySort(findFlow, sort)
        }
        return findFlow.skip(pageable.offset.toInt()).limit(pageable.pageSize).toList()
    }

    override suspend fun findAll(sort: Sort): List<T> {
        var findFlow = collection.find()
        findFlow = applySort(findFlow, sort)
        return findFlow.toList()
    }

    override suspend fun deleteById(id: String): Long {
        val result = collection.deleteOne(Filters.eq("_id", id))
        return result.deletedCount
    }

    override suspend fun delete(entity: T): Long {
        if (entity.id != null) {
            val result = collection.deleteOne(Filters.eq("_id", entity.id!!))
            return result.deletedCount
        }
        return 0L
    }

    override suspend fun deleteAllInBatch(entities: Iterable<T>): Long {
        val ids = entities.mapNotNull { it.id }
        if (ids.isEmpty()) {
            return 0L
        }
        val result = collection.deleteMany(Filters.`in`("_id", ids))
        return result.deletedCount
    }

    override suspend fun deleteAll(): Long {
        val result = collection.deleteMany(Document())
        return result.deletedCount
    }

    override suspend fun count(): Long {
        return collection.countDocuments()
    }

    override suspend fun findByProperty(property: String, value: Any): List<T> {
        return collection.find(Filters.eq(property, value)).toList()
    }

    override suspend fun countByProperty(property: String, value: Any): Long {
        return collection.countDocuments(Filters.eq(property, value))
    }

    override suspend fun deleteByProperty(property: String, value: Any): Long {
        val result = collection.deleteMany(Filters.eq(property, value))
        return result.deletedCount
    }

    override suspend fun existsByProperty(property: String, value: Any): Boolean {
        return collection.countDocuments(Filters.eq(property, value)) > 0
    }

    override suspend fun executeCustomQuery(query: String, params: Map<String, Any>): List<T> {
        val filterDocument = Document.parse(query)
        return collection.find(filterDocument).toList()
    }

    override suspend fun findByQuerySpec(
        querySpec: QuerySpec,
        parameters: List<Any>,
        sort: Sort?,
        limit: Int?,
        offset: Long?,
        distinct: Boolean,
        projectionClass: KClass<*>?
    ): List<Any> {
        val filter = buildMongoFilter(querySpec, parameters)
        var findFlow = collection.find(filter)

        sort?.let { findFlow = applySort(findFlow, it) }
        offset?.let { findFlow = findFlow.skip(it.toInt()) }
        limit?.let { findFlow = findFlow.limit(it) }

        if (distinct) {
            // Note: distinct in MongoDB works on a specific field, not the entire document.
            // This implementation will return distinct values for the first property in the query spec.
            val distinctField = querySpec.conditions.firstOrNull()?.property
            if (distinctField != null) {
                return collection.distinct(distinctField, filter, Any::class.java).toList()
            }
        }

        projectionClass?.let {
            if (it != entityClass) {
                val projection = Projections.fields(*it.constructors.first().parameters.map { param -> Projections.include(param.name) }.toTypedArray())
                findFlow = findFlow.projection(projection)
            }
        }

        return findFlow.toList()
    }

    override suspend fun countByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long {
        val filter = buildMongoFilter(querySpec, parameters)
        return collection.countDocuments(filter)
    }

    override suspend fun deleteByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long {
        val filter = buildMongoFilter(querySpec, parameters)
        val result = collection.deleteMany(filter)
        return result.deletedCount
    }

    override suspend fun existsByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Boolean {
        val filter = buildMongoFilter(querySpec, parameters)
        return collection.countDocuments(filter) > 0
    }

    override suspend fun aggregate(queryInfo: QueryInfo, parameters: List<Any>): Any? {
        val filter = buildMongoFilter(queryInfo.querySpec, parameters)
        val aggregationFunction = queryInfo.aggregationFunction ?: throw IllegalArgumentException("Aggregation function cannot be null")

        if (aggregationFunction == AggregationFunction.COUNT) {
            return collection.countDocuments(filter)
        }

        val groupStage = when (aggregationFunction) {
            AggregationFunction.SUM -> Aggregates.group(null, Accumulators.sum("sum", "\${queryInfo.property}"))
            AggregationFunction.AVG -> Aggregates.group(null, Accumulators.avg("avg", "\${queryInfo.property}"))
            AggregationFunction.MIN -> Aggregates.group(null, Accumulators.min("min", "\${queryInfo.property}"))
            AggregationFunction.MAX -> Aggregates.group(null, Accumulators.max("max", "\${queryInfo.property}"))
            else -> throw IllegalStateException("Should not happen")
        }

        val pipeline = listOf(Aggregates.match(filter), groupStage)
        val result = collection.aggregate(pipeline, Document::class.java).firstOrNull()
        return result?.get(aggregationFunction.name.lowercase())
    }

    private fun applySort(findFlow: FindFlow<T>, sort: Sort): FindFlow<T> {
        val sortFields = sort.orders.map { order ->
            if (order.direction == Sort.Direction.ASC) {
                Sorts.ascending(order.property)
            } else {
                Sorts.descending(order.property)
            }
        }
        return findFlow.sort(Sorts.orderBy(sortFields))
    }

    private fun buildMongoFilter(querySpec: QuerySpec, parameters: List<Any>): Bson {
        if (querySpec.conditions.isEmpty()) {
            return Document()
        }

        var paramIndex = 0
        val filters = querySpec.conditions.map { condition ->
            val value = if (paramIndex < parameters.size) parameters[paramIndex++] else null
            when (condition.operator) {
                QueryOperator.EQUALS -> Filters.eq(condition.property, value)
                QueryOperator.GREATER_THAN -> Filters.gt(condition.property, value as Any)
                QueryOperator.LESS_THAN -> Filters.lt(condition.property, value as Any)
                QueryOperator.GREATER_THAN_EQUAL -> Filters.gte(condition.property, value as Any)
                QueryOperator.LESS_THAN_EQUAL -> Filters.lte(condition.property, value as Any)
                QueryOperator.CONTAINING -> Filters.regex(condition.property, ".*$value.*")
                QueryOperator.CONTAINING_IGNORE_CASE -> Filters.regex(condition.property, ".*$value.*", "i")
                QueryOperator.STARTING_WITH -> Filters.regex(condition.property, "^$value")
                QueryOperator.ENDING_WITH -> Filters.regex(condition.property, "$value$")
                QueryOperator.IS_NULL -> Filters.eq(condition.property, null)
                QueryOperator.IS_NOT_NULL -> Filters.ne(condition.property, null)
                QueryOperator.IN -> Filters.`in`(condition.property, value as List<*>)
                QueryOperator.NOT_IN -> Filters.nin(condition.property, value as List<*>)
                QueryOperator.BETWEEN -> {
                    val values = value as List<*>
                    Filters.and(Filters.gte(condition.property, values[0] as Any), Filters.lte(condition.property, values[1] as Any))
                }
                QueryOperator.NOT -> Filters.ne(condition.property, value)
                QueryOperator.IS_EMPTY -> Filters.eq(condition.property, "")
                QueryOperator.IS_NOT_EMPTY -> Filters.ne(condition.property, "")
                QueryOperator.TRUE -> Filters.eq(condition.property, true)
                QueryOperator.FALSE -> Filters.eq(condition.property, false)
            }
        }

        return when (querySpec.logicalOperator) {
            LogicalOperator.AND -> Filters.and(filters)
            LogicalOperator.OR -> Filters.or(filters)
        }
    }
}
