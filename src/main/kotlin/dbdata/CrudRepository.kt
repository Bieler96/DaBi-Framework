package dbdata

interface CrudRepository<T : Entity<ID>, ID> {
	suspend fun save(entity: T): T
	suspend fun saveAll(entities: Iterable<T>): List<T>
	suspend fun findById(id: ID): T?
	suspend fun findAll(): List<T>
	suspend fun deleteById(id: ID): Long
	suspend fun delete(entity: T): Long
	suspend fun deleteAllInBatch(entities: Iterable<T>): Long
	suspend fun deleteAll(): Long
	suspend fun count(): Long
	suspend fun existsById(id: ID): Boolean
}