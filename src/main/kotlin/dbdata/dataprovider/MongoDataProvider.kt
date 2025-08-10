
package dbdata.dataprovider

import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoCollection
import dbdata.Entity
import dbdata.Auditable
import dbdata.query.QueryOperator
import dbdata.query.QuerySpec
import dbdata.query.Pageable
import dbdata.query.Sort
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import java.time.LocalDateTime
import org.bson.Document
import com.mongodb.client.model.Updates

class MongoDataProvider<T : Entity<String>>(
	private val collection: MongoCollection<T>,
	private val entityClass: kotlin.reflect.KClass<T>
): DataProvider<T, String>() {
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
			collection.replaceOne(
				org.bson.Document("_id", entity.id),
				entity
			)
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
		return collection.find(org.bson.Document("_id", id)).firstOrNull()
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
		val result = collection.deleteOne(org.bson.Document("_id", id))
		return result.deletedCount
	}

	override suspend fun delete(entity: T): Long {
		if (entity.id != null) {
			val result = collection.deleteOne(org.bson.Document("_id", entity.id))
			return result.deletedCount
		}
		return 0L
	}

	override suspend fun deleteAllInBatch(entities: Iterable<T>): Long {
		val ids = entities.mapNotNull { it.id }
		if (ids.isEmpty()) {
			return 0L
		}
		val result = collection.deleteMany(org.bson.Document("_id", org.bson.Document("\$in", ids)))
		return result.deletedCount
	}

	override suspend fun deleteAll(): Long {
		val result = collection.deleteMany(org.bson.Document())
		return result.deletedCount
	}

	override suspend fun count(): Long {
		return collection.countDocuments()
	}

	override suspend fun findByProperty(property: String, value: Any): List<T> {
		return collection.find(org.bson.Document(property, value)).toList()
	}

	override suspend fun countByProperty(property: String, value: Any): Long {
		return collection.countDocuments(org.bson.Document(property, value))
	}

	override suspend fun deleteByProperty(property: String, value: Any): Long {
		val result = collection.deleteMany(org.bson.Document(property, value))
		return result.deletedCount
	}

	override suspend fun existsByProperty(property: String, value: Any): Boolean {
		return collection.countDocuments(
			org.bson.Document(property, value),
			com.mongodb.client.model.CountOptions().limit(1)
		) > 0
	}

	override suspend fun executeCustomQuery(query: String, params: Map<String, Any>): List<T> {
		// MongoDB Query Ausführung basierend auf Query String
		// Du könntest hier eine DSL implementieren oder JSON Queries verwenden
		return emptyList()
	}

	override suspend fun findByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): List<T> {
		// TODO: Implement advanced MongoDB queries
		// For now, fallback to simple implementation
		if (querySpec.conditions.size == 1 && querySpec.conditions[0].operator == QueryOperator.EQUALS) {
			return findByProperty(querySpec.conditions[0].property, parameters[0])
		}
		return emptyList()
	}

	override suspend fun countByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long {
		// TODO: Implement advanced MongoDB queries
		if (querySpec.conditions.size == 1 && querySpec.conditions[0].operator == QueryOperator.EQUALS) {
			return countByProperty(querySpec.conditions[0].property, parameters[0])
		}
		return 0
	}

	override suspend fun deleteByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long {
		// TODO: Implement advanced MongoDB queries
		if (querySpec.conditions.size == 1 && querySpec.conditions[0].operator == QueryOperator.EQUALS) {
			return deleteByProperty(querySpec.conditions[0].property, parameters[0])
		}
		return 0
	}

	override suspend fun existsByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Boolean {
		// TODO: Implement advanced MongoDB queries
		if (querySpec.conditions.size == 1 && querySpec.conditions[0].operator == QueryOperator.EQUALS) {
			return existsByProperty(querySpec.conditions[0].property, parameters[0])
		}
		return false
	}

	private fun applySort(findFlow: FindFlow<T>, sort: Sort): FindFlow<T> {
		val sortDocument = org.bson.Document()
		sort.orders.forEach { order ->
			sortDocument[order.property] = if (order.direction == Sort.Direction.ASC) 1 else -1
		}
		return findFlow.sort(sortDocument)
	}
}
