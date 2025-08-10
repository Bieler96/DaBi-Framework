package dbdata

import com.mongodb.kotlin.client.coroutine.MongoCollection
import dbdata.dataprovider.ExposedDataProvider
import dbdata.dataprovider.MongoDataProvider
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.reflect.KClass

class DataRepositoryConfiguration {
	private val repositories = mutableMapOf<KClass<*>, Any>()
	private val proxyFactory = RepositoryProxyFactory()

	fun <T : Any, E : Entity<ID>, ID> registerExposedRepository(
		repositoryClass: KClass<T>,
		table: Table,
		entityClass: KClass<E>,
		idColumn: Column<ID>,
		database: Database
	) {
		val dataProvider = ExposedDataProvider(table, entityClass, idColumn, database)
		val repository = proxyFactory.create(repositoryClass, dataProvider)
		repositories[repositoryClass] = repository
	}

	fun <T : Any, E : Entity<String>> registerMongoRepository(
		repositoryClass: KClass<T>,
		collection: MongoCollection<E>,
		entityClass: KClass<E>
	) {
		val dataProvider = MongoDataProvider(collection, entityClass)
		val repository = proxyFactory.create(repositoryClass, dataProvider)
		repositories[repositoryClass] = repository
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getRepository(repositoryClass: KClass<T>): T {
		return repositories[repositoryClass] as? T
			?: throw IllegalArgumentException("Repository not registered: ${repositoryClass.simpleName}")
	}
}