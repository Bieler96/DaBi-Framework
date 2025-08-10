package dbdata

import dbdata.dataprovider.DataProvider
import dbdata.query.QueryInfo
import dbdata.query.QueryMethodParser
import dbdata.query.QueryType
import kotlinx.coroutines.runBlocking
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

class RepositoryInvocationHandler<T : Entity<ID>, ID>(
	private val dataProvider: DataProvider<T, ID>,
	private val queryParser: QueryMethodParser
) : InvocationHandler {

	@Suppress("UNCHECKED_CAST")
	override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
		val methodName = method.name
		val parameters = args ?: emptyArray()

		// Handle Object methods
		when (methodName) {
			"toString" -> return "Repository Proxy for ${dataProvider::class.simpleName}"
			"equals" -> return proxy === (parameters.getOrNull(0))
			"hashCode" -> return System.identityHashCode(proxy)
		}

		// For suspend functions, we need to use runBlocking or return a coroutine
		// This is a simplified approach - in production you might want a more sophisticated solution
		return runBlocking {
			when (methodName) {
				"save" -> dataProvider.save(parameters[0] as T)
				"saveAll" -> dataProvider.saveAll(parameters[0] as Iterable<T>)
				"findById" -> dataProvider.findById(parameters[0] as ID)
				"findAll" -> dataProvider.findAll()
				"deleteById" -> dataProvider.deleteById(parameters[0] as ID)
				"delete" -> dataProvider.delete(parameters[0] as T)
				"deleteAll" -> dataProvider.deleteAll()
				"deleteAllInBatch" -> dataProvider.deleteAllInBatch(parameters[0] as Iterable<T>)
				"count" -> dataProvider.count()
				"existsById" -> dataProvider.existsByProperty("id", parameters[0])
				else -> {
					val queryInfo = queryParser.parseMethodName(methodName)
					handleCustomQuery(queryInfo, parameters, method)
				}
			}
		}
	}

	private suspend fun handleCustomQuery(
		queryInfo: QueryInfo,
		parameters: Array<out Any>,
		method: Method
	): Any? {
		return when (queryInfo.type) {
			QueryType.FIND -> {
				val results = dataProvider.findByQuerySpec(queryInfo.querySpec, parameters.toList())

				// Check if method should return single entity or list
				if (method.kotlinFunction?.let { queryParser.shouldReturnSingle(method.name, it.returnType) } == true) {
					results.firstOrNull()
				} else {
					results
				}
			}

			QueryType.COUNT -> {
				dataProvider.countByQuerySpec(queryInfo.querySpec, parameters.toList())
			}

			QueryType.DELETE -> {
				dataProvider.deleteByQuerySpec(queryInfo.querySpec, parameters.toList())
			}

			QueryType.EXISTS -> {
				dataProvider.existsByQuerySpec(queryInfo.querySpec, parameters.toList())
			}

			QueryType.CUSTOM -> {
				val queryAnnotation = method.getAnnotation(Query::class.java)
				if (queryAnnotation != null) {
					val paramMap = parameters.mapIndexed { index, value ->
						"param$index" to value
					}.toMap()
					dataProvider.executeCustomQuery(queryAnnotation.value, paramMap)
				} else {
					throw UnsupportedOperationException("Custom method without @Query annotation: ${method.name}")
				}
			}
		}
	}
}