package mapping

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@DslMarker
annotation class MapperDsl

@MapperDsl
class MappingBlock<S : Any, T : Any> {
	private val explicitMappings = mutableMapOf<String, (S) -> Any?>()


	infix fun <V> String.from(extractor: (S) -> V) {
		explicitMappings[this] = extractor
	}

	internal fun build(source: S, targetClass: KClass<T>): Map<String, Any?> {
		val result = mutableMapOf<String, Any?>()
		val sourceProps = source::class.memberProperties.associateBy { it.name }
		val targetCtor = targetClass.primaryConstructor ?: error("No primary constructor for ${targetClass.simpleName}")

		for (param in targetCtor.parameters) {
			val name = param.name ?: continue
			val explicit = explicitMappings[name]

			if (explicit != null) {
				result[name] = explicit(source)
				continue
			}

			val prop = sourceProps[name] ?: continue
			val value = prop.getter.call(source)

			val sourceClass = prop.returnType.classifier as? KClass<Any>
			val targetClass = param.type.classifier as? KClass<Any>
			val mapper = if (sourceClass != null && targetClass != null) {
				MapperRegistry.get<Any, Any>()
			} else null

			if (mapper != null && value != null) {
				result[name] = (mapper as Mapper<Any, Any>).toDto(value)
			} else {
				result[name] = value
			}
		}
		return result
	}
}

class MapperBuilder<E : Any, D : Any>(
	private val entityClass: KClass<E>,
	private val dtoClass: KClass<D>,
	private val registry: MapperRegistry
) {
	private var entityToDtoBlock: (MappingBlock<E, D>.() -> Unit)? = null
	private var dtoToEntityBlock: (MappingBlock<D, E>.() -> Unit)? = null

	fun entityToDto(block: MappingBlock<E, D>.() -> Unit = {}) {
		entityToDtoBlock = block
	}

	fun dtoToEntity(block: MappingBlock<D, E>.() -> Unit = {}) {
		dtoToEntityBlock = block
	}

	fun build(): Mapper<E, D> {
		val entityToDto = entityToDtoBlock ?: {}
		val dtoToEntity = dtoToEntityBlock ?: {}

		return object : Mapper<E, D> {
			override fun toDto(entity: E): D {
				val data = MappingBlock<E, D>()
					.apply(entityToDto)
					.build(entity, dtoClass)

				val ctor = dtoClass.primaryConstructor!!
				val args = ctor.parameters.associateWith { data[it.name] }
				return ctor.callBy(args)
			}

			override fun toEntity(dto: D): E {
				val data = MappingBlock<D, E>()
					.apply(dtoToEntity)
					.build(dto, entityClass)

				val ctor = entityClass.primaryConstructor!!
				val args = ctor.parameters.associateWith { data[it.name] }
				return ctor.callBy(args)
			}
		}
	}
}

inline fun <reified E : Any, reified D : Any> mapper(
	registry: MapperRegistry,
	noinline block: MapperBuilder<E, D>.() -> Unit = {}
): Mapper<E, D> = MapperBuilder(E::class, D::class, registry).apply(block).build()