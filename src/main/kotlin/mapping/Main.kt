package mapping

fun main() {
	MapperRegistry.autoDiscover("mapping")

	// Register DSL-based mapper
	MapperRegistry.registerMapper<ProductEntity, ProductDTO> {
		entityToDto {
			"id" from { it.productId.toString() }
			"price" from { it.priceInCents / 100.0 }
		}
		dtoToEntity {
			"productId" from { it.id.toLong() }
			"priceInCents" from { (it.price * 100).toInt() }
		}
	}

	val entity = UserEntity("1", "David", "david@example.com")
	println(entity)

	val mapViaRegistry = entity.mapViaRegistry<UserEntity, UserDTO>()
	println(mapViaRegistry)

	val dto = UserMapper.toDto(entity)

	// Test DSL-based mapper via Registry
	val product = ProductEntity(123, "Laptop", 99999)
	println("\nProduct Entity: $product")
	val productDto = product.mapViaRegistry<ProductEntity, ProductDTO>()
	println("Product DTO: $productDto")
	val productBack = productDto.mapBackViaRegistry<ProductEntity, ProductDTO>()
	println("Product Entity (back): $productBack")

	// Test DSL-based mapper WITHOUT Registry (standalone)
	val orderMapper = mapper<OrderEntity, OrderDTO>(MapperRegistry) {
		entityToDto {
			"orderId" from { "ORD-${it.id}" }
			"totalAmount" from { it.amount / 100.0 }
		}
		dtoToEntity {
			"id" from { it.orderId.removePrefix("ORD-").toLong() }
			"amount" from { (it.totalAmount * 100).toInt() }
		}
	}

	val order = OrderEntity(42, "Desk", 15000)
	println("\nOrder Entity: $order")
	val orderDto = order.mapWith(orderMapper)
	println("Order DTO: $orderDto")
	val orderBack = orderDto.mapBackWith(orderMapper)
	println("Order Entity (back): $orderBack")
}

data class UserEntity(
	val id: String,
	val name: String,
	val email: String
)

data class UserDTO(
	val id: Long,
	val name: String,
	val email: String?
)

object UserMapper : BaseMapper<UserEntity, UserDTO>(
	entityClass = UserEntity::class,
	dtoClass = UserDTO::class
) {
	override fun toDto(entity: UserEntity): UserDTO {
		return UserDTO(
			id = entity.id.toLong(),
			name = entity.name,
			email = "dummy-${entity.email}"
		)
	}

	override fun toEntity(dto: UserDTO): UserEntity {
		return UserEntity(
			id = dto.id.toString(),
			name = dto.name,
			email = dto.email ?: ""
		)
	}
}

data class ProductEntity(
	val productId: Long,
	val name: String,
	val priceInCents: Int
)

data class ProductDTO(
	val id: String,
	val name: String,
	val price: Double
)

data class OrderEntity(
	val id: Long,
	val description: String,
	val amount: Int
)

data class OrderDTO(
	val orderId: String,
	val description: String,
	val totalAmount: Double
)