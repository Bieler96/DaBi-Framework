package dbdata.dataprovider

import dbdata.Entity
import dbdata.query.QuerySpec
import dbdata.query.Pageable
import dbdata.query.Sort

abstract class DataProvider<T : Entity<ID>, ID> {
	abstract suspend fun save(entity: T): T
	abstract suspend fun saveAll(entities: Iterable<T>): List<T>
	abstract suspend fun findById(id: ID): T?
	abstract suspend fun findAll(): List<T>
	abstract suspend fun findAll(pageable: Pageable): List<T>
	abstract suspend fun findAll(sort: Sort): List<T>
	abstract suspend fun deleteById(id: ID): Long
	abstract suspend fun delete(entity: T): Long
	abstract suspend fun deleteAllInBatch(entities: Iterable<T>): Long
	abstract suspend fun deleteAll(): Long
	abstract suspend fun count(): Long
	abstract suspend fun findByProperty(property: String, value: Any): List<T>
	abstract suspend fun countByProperty(property: String, value: Any): Long
	abstract suspend fun deleteByProperty(property: String, value: Any): Long
	abstract suspend fun existsByProperty(property: String, value: Any): Boolean
	abstract suspend fun executeCustomQuery(query: String, params: Map<String, Any>): List<T>

	abstract suspend fun findByQuerySpec(querySpec: QuerySpec, parameters: List<Any>, sort: Sort? = null, limit: Int? = null, offset: Long? = null, distinct: Boolean = false): List<T>
	abstract suspend fun countByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long
	abstract suspend fun deleteByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Long
	abstract suspend fun existsByQuerySpec(querySpec: QuerySpec, parameters: List<Any>): Boolean
}