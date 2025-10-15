package mapping

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

interface Mapper<E, D> {
	fun toDto(entity: E): D
	fun toEntity(dto: D): E

	fun toDtoList(entities: Iterable<E>): List<D> = entities.map { toDto(it) }
	fun toEntityList(dtos: Iterable<D>): List<E> = dtos.map { toEntity(it) }

	fun updateEntity(entity: E, dto: D): E = toEntity(dto)
}

abstract class BaseMapper<E : Any, D : Any>(
	private val entityClass: KClass<E>,
	private val dtoClass: KClass<D>
) : Mapper<E, D> {
	override fun toDto(entity: E): D = entity.mapTo(dtoClass)
	override fun toEntity(dto: D): E = dto.mapTo(entityClass)
	override fun updateEntity(entity: E, dto: D): E = entity.updateWith(dto)
}

object MapperRegistry {
	val mappers = mutableMapOf<Pair<KClass<*>, KClass<*>>, Mapper<*, *>>()

	fun <E : Any, D : Any> register(entity: KClass<E>, dto: KClass<D>, mapper: Mapper<E, D>) {
		mappers[entity to dto] = mapper
	}

	inline fun <reified E : Any, reified D : Any> registerMapper(
		noinline block: MapperBuilder<E, D>.() -> Unit = {}
	) {
		val mapper = mapper(this, block)
		register(E::class, D::class, mapper)
	}

	@Suppress("UNCHECKED_CAST")
	inline fun <reified E : Any, reified D : Any> get(): Mapper<E, D>? =
		mappers[E::class to D::class] as? Mapper<E, D>

	fun findMapper(from: KClass<*>, to: KClass<*>): Mapper<*, *>? =
		mappers[from to to] ?: mappers[to to from]

	// 🔄 automatische Wert-Konvertierung (rekursiv)
	fun autoMapValue(value: Any?, targetType: KType): Any? {
		if (value == null) return null
		val targetClassifier = targetType.classifier

		// 1️⃣ Collection-Mapping (List, Set)
		if (value is Iterable<*> && targetClassifier is KClass<*> && Iterable::class.java.isAssignableFrom(targetClassifier.java)) {
			val elementType = targetType.arguments.firstOrNull()?.type
			if (elementType != null) {
				return value.mapNotNull { elem -> autoMapValue(elem, elementType) }
			}
			return value
		}

		// 2️⃣ Map-Mapping
		if (value is Map<*, *> && targetClassifier == Map::class) {
			val (keyType, valType) = targetType.arguments.mapNotNull { it.type }
			return value.mapKeys { autoMapValue(it.key, keyType) }
				.mapValues { autoMapValue(it.value, valType) }
		}

		// 3️⃣ Nested-Mapper
		if (targetClassifier is KClass<*>) {
			val subMapper = findMapper(value::class, targetClassifier)
			if (subMapper != null) {
				@Suppress("UNCHECKED_CAST")
				return (subMapper as Mapper<Any, Any>).toDto(value)
			}
		}

		// 4️⃣ Fallback: direkt übernehmen
		return value
	}

	fun autoDiscover(packageName: String) {
		val classLoader = Thread.currentThread().contextClassLoader
		val path = packageName.replace('.', '/')
		val resources = classLoader.getResources(path)

		while (resources.hasMoreElements()) {
			val resource = resources.nextElement()
			val files = java.io.File(resource.file)
			if (files.exists()) {
				files.walkTopDown()
					.filter { it.isFile && it.extension == "class" }
					.forEach { file ->
						val className = buildString {
							append(packageName)
							append('.')
							append(file.nameWithoutExtension)
						}
						try {
							val clazz = Class.forName(className).kotlin
							registerIfMapper(clazz)
						} catch (_: Throwable) {
							// Ignoriere Klassen, die nicht geladen werden können
						}
					}
			}
		}
	}

	private fun registerIfMapper(clazz: KClass<*>) {
		if (clazz.objectInstance != null) {
			val instance = clazz.objectInstance!!
			if (instance is Mapper<*, *>) {
				val supertypes = clazz.supertypes
					.mapNotNull { it.arguments }
					.flatten()
					.mapNotNull { it.type?.classifier as? KClass<*> }

				if (supertypes.size >= 2) {
					val entityType = supertypes[0]
					val dtoType = supertypes[1]
					println("✅ Auto-registered mapper: ${clazz.simpleName} for ${entityType.simpleName} <-> ${dtoType.simpleName}")
					@Suppress("UNCHECKED_CAST")
					register(entityType as KClass<Any>, dtoType as KClass<Any>, instance as Mapper<Any, Any>)
				}
			}
		}
	}

	inline fun <reified E : Any, reified D : Any> mapToDto(entity: E): D =
		get<E, D>()?.toDto(entity)
			?: error("No mapper registered for ${E::class.simpleName} → ${D::class.simpleName}")

	inline fun <reified E : Any, reified D : Any> mapToEntity(dto: D): E =
		get<E, D>()?.toEntity(dto)
			?: error("No mapper registered for ${D::class.simpleName} → ${E::class.simpleName}")
}

inline fun <reified T : Any> Any.mapTo(): T = mapTo(T::class)

@Suppress("UNCHECKED_CAST")
fun <T : Any> Any.mapTo(targetClass: KClass<T>): T {
	val targetConstructor = targetClass.constructors.firstOrNull()
		?: error("No suitable constructor found for ${targetClass.simpleName}")

	val sourceProps = this::class.members
		.filterIsInstance<KProperty1<Any, *>>()
		.associateBy { it.name }

	val args = targetConstructor.parameters.associateWith { param ->
		sourceProps[param.name]?.get(this)
	}

	return targetConstructor.callBy(args) as T
}

@Suppress("UNCHECKED_CAST")
fun <E : Any, D : Any> E.updateWith(dto: D): E {
	val kClass = this::class
	val copyFunction = kClass.members.firstOrNull { it.name == "copy" } ?: return this

	val dtoProps = dto::class.members
		.filterIsInstance<KProperty1<D, *>>()
		.associateBy { it.name }

	val args = copyFunction.parameters.associateWith { param ->
		val value = dtoProps[param.name]?.get(dto)
		value ?: param.defaultValueOrNull()
	}

	return copyFunction.callBy(args) as E
}

private fun KParameter.defaultValueOrNull(): Any? = if (isOptional) null else null

// Convenience Mapping Shortcuts
inline fun <E, D> E.mapWith(mapper: Mapper<E, D>): D = mapper.toDto(this)
inline fun <E, D> D.mapBackWith(mapper: Mapper<E, D>): E = mapper.toEntity(this)

inline fun <E, D> Iterable<E>.mapWith(mapper: Mapper<E, D>): List<D> = mapper.toDtoList(this)
inline fun <E, D> Iterable<D>.mapBackWith(mapper: Mapper<E, D>): List<E> = mapper.toEntityList(this)

inline fun <reified E : Any, reified D : Any> E.mapViaRegistry(): D {
	val mapper = MapperRegistry.get<E, D>()
		?: error("No mapper registered for ${E::class.simpleName} -> ${D::class.simpleName}")
	return mapper.toDto(this)
}

inline fun <reified E : Any, reified D : Any> D.mapBackViaRegistry(): E {
	val mapper = MapperRegistry.get<E, D>()
		?: error("No mapper registered for ${D::class.simpleName} -> ${E::class.simpleName}")
	return mapper.toEntity(this)
}