package dbdata

import dbdata.dataprovider.DataProvider
import dbdata.query.QueryMethodParser
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class RepositoryProxyFactory {
	private val queryParser = QueryMethodParser()

	fun <T : Any, E : Entity<ID>, ID> create(
		repositoryInterface: KClass<T>,
		dataProvider: DataProvider<E, ID>
	): T {
		val handler = RepositoryInvocationHandler(dataProvider, queryParser)

		@Suppress("UNCHECKED_CAST")
		return Proxy.newProxyInstance(
			repositoryInterface.java.classLoader,
			arrayOf(repositoryInterface.java),
			handler
		) as T
	}
}