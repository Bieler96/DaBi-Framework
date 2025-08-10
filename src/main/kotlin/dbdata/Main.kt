package dbdata

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class User(
	override val id: Long? = null,
	val name: String,
	val email: String,
	val age: Int
) : Entity<Long>

@Repository
interface UserRepository : CrudRepository<User, Long> {
	// Simple queries
	suspend fun findByEmail(email: String): User?
	suspend fun findByName(name: String): List<User>
	suspend fun countByAge(age: Int): Long
	suspend fun deleteByEmail(email: String): Long
	suspend fun existsByEmail(email: String): Boolean

	// Advanced compound queries
	suspend fun findByNameAndAge(name: String, age: Int): List<User>
	suspend fun findByNameOrEmail(name: String, email: String): List<User>
	suspend fun findByAgeGreaterThan(minAge: Int): List<User>
	suspend fun findByAgeLessThanEqual(maxAge: Int): List<User>
	suspend fun findByEmailContaining(substring: String): List<User>
	suspend fun findByEmailContainingIgnoreCase(substring: String): List<User>
	suspend fun findByNameStartingWith(prefix: String): List<User>
	suspend fun findByNameEndingWith(suffix: String): List<User>
	suspend fun findByAgeIn(ages: List<Int>): List<User>
	suspend fun findByAgeNotIn(ages: List<Int>): List<User>
	suspend fun findByAgeBetween(minAge: Int, maxAge: Int): List<User>

	// Complex compound queries
	suspend fun findByNameAndAgeGreaterThan(name: String, minAge: Int): List<User>
	suspend fun findByEmailContainingAndAgeLessThan(emailPart: String, maxAge: Int): List<User>
	suspend fun countByAgeGreaterThanAndNameStartingWith(minAge: Int, namePrefix: String): Long

	@Query("SELECT * FROM users WHERE age > :minAge")
	suspend fun findUsersOlderThan(minAge: Int): List<User>
}

object UsersTable : Table("users") {
	val id = long("id").autoIncrement()
	val name = varchar("name", 100)
	val email = varchar("email", 200)
	val age = integer("age")

	override val primaryKey = PrimaryKey(id)
}

fun setupRepositories(): DataRepositoryConfiguration {
	val config = DataRepositoryConfiguration()

	// Exposed Setup
	val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
	transaction(database) {
		SchemaUtils.create(UsersTable)
	}
	config.registerExposedRepository(
		UserRepository::class,
		UsersTable,
		User::class,
		UsersTable.id,
		database
	)

	return config
}

suspend fun main() {
	val config = setupRepositories()
	val userRepository = config.getRepository(UserRepository::class)

	val user = userRepository.save(User(name = "John", email = "john@example.com", age = 30))
	val foundUser = userRepository.findByEmail("john@example.com")
	val allUsers = userRepository.findAll()
	val count = userRepository.countByAge(30)
	val foundByName = userRepository.findByNameStartingWith("Jo")

	println("User: $user")
	println("Found User: $foundUser")
	println("All Users: $allUsers")
	println("Count: $count")
	println("Found by Name: $foundByName")
}