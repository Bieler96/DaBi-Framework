package dbdata

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import dbdata.query.PageRequest
import dbdata.query.Sort

data class User(
	override val id: Long? = null,
	val name: String,
	val email: String,
	val age: Int,
	override var createdAt: LocalDateTime? = null,
	override var updatedAt: LocalDateTime? = null,
	override var createdBy: String? = null,
	override var updatedBy: String? = null
) : Entity<Long>

data class Post(
	override val id: Long? = null,
	val title: String,
	val content: String,
	val userId: Long,
	override var createdAt: LocalDateTime? = null,
	override var updatedAt: LocalDateTime? = null,
	override var createdBy: String? = null,
	override var updatedBy: String? = null
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

@Repository
interface PostRepository : CrudRepository<Post, Long> {
	suspend fun findByUserId(userId: Long): List<Post>
	suspend fun findByTitleContaining(titlePart: String): List<Post>
}

object UsersTable : Table("users") {
	val id = long("id").autoIncrement()
	val name = varchar("name", 100)
	val email = varchar("email", 200)
	val age = integer("age")
	val createdAt = long("created_at").nullable()
	val updatedAt = long("updated_at").nullable()
	val createdBy = varchar("created_by", 255).nullable()
	val updatedBy = varchar("updated_by", 255).nullable()

	override val primaryKey = PrimaryKey(id)
}

object PostsTable : Table("posts") {
	val id = long("id").autoIncrement()
	val title = varchar("title", 255)
	val content = text("content")
	val userId = long("user_id").references(UsersTable.id)
	val createdAt = long("created_at").nullable()
	val updatedAt = long("updated_at").nullable()
	val createdBy = varchar("created_by", 255).nullable()
	val updatedBy = varchar("updated_by", 255).nullable()
	override val primaryKey = PrimaryKey(id)
}

fun setupRepositories(): DataRepositoryConfiguration {
	val config = DataRepositoryConfiguration()

	// Exposed Setup
	val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
	transaction(database) {
		SchemaUtils.create(UsersTable, PostsTable) // Create both tables
	}
	config.registerExposedRepository(
		UserRepository::class,
		UsersTable,
		User::class,
		UsersTable.id,
		database
	)
	config.registerExposedRepository(
		PostRepository::class,
		PostsTable,
		Post::class,
		PostsTable.id,
		database
	)

	return config
}

suspend fun main() {
	val config = setupRepositories()
	val userRepository = config.getRepository(UserRepository::class)
	val postRepository = config.getRepository(PostRepository::class)

	val user1 = userRepository.save(User(name = "John Doe", email = "john@example.com", age = 30))
	val user2 = userRepository.save(User(name = "Jane Smith", email = "jane@example.com", age = 25))

	println("Saved User1: $user1")
	println("Saved User2: $user2")

	// Save posts for user1
	val post1 = postRepository.save(Post(title = "My First Post", content = "Hello World!", userId = user1.id!!))
	val post2 =
		postRepository.save(Post(title = "Another Post", content = "This is my second post.", userId = user1.id!!))

	// Save a post for user2
	val post3 = postRepository.save(Post(title = "Jane's Post", content = "A post by Jane.", userId = user2.id!!))

	println("Saved Post1: $post1")
	println("Saved Post2: $post2")
	println("Saved Post3: $post3")

	// Find posts by user1
	val johnsPosts = postRepository.findByUserId(user1.id!!)
	println("John's Posts: $johnsPosts")

	// Find posts by title containing
	val postsWithHello = postRepository.findByTitleContaining("Hello")
	println("Posts with 'Hello': $postsWithHello")

	// Demonstrate finding user by email (existing functionality)
	val foundUser = userRepository.findByEmail("john@example.com")
	println("Found User by Email: $foundUser")

	// Demonstrate finding all users (existing functionality)
	val allUsers = userRepository.findAll()
	println("All Users: $allUsers")

	// Demonstrate finding all posts
	val allPosts = postRepository.findAll()
	println("All Posts: $allPosts")

	// Demonstrate pagination
	println(
		"""
--- Paginated Users (Page 0, Size 2) ---"""
	)
	val page0 = userRepository.findAll(PageRequest(pageNumber = 0, pageSize = 2))
	println("Page 0: $page0")

	println(
		"""
--- Paginated Users (Page 1, Size 2) ---"""
	)
	val page1 = userRepository.findAll(PageRequest(pageNumber = 1, pageSize = 2))
	println("Page 1: $page1")

	// Demonstrate sorting
	println(
		"""
--- Users sorted by Name ASC ---"""
	)
	val sortedByNameAsc = userRepository.findAll(Sort(property = "name", direction = Sort.Direction.ASC))
	println("Sorted by Name ASC: $sortedByNameAsc")

	println(
		"""
--- Users sorted by Age DESC ---"""
	)
	val sortedByAgeDesc = userRepository.findAll(Sort(property = "age", direction = Sort.Direction.DESC))
	println("Sorted by Age DESC: $sortedByAgeDesc")

	// Example of a query that *would* require joins if implemented
	// suspend fun findPostsByUserEmail(email: String): List<Post>
	// This method would currently fail because ExposedDataProvider doesn't handle joins yet.
	// To achieve this, you would first find the user by email, then find posts by userId.
	val userByEmailForPosts = userRepository.findByEmail("jane@example.com")
	if (userByEmailForPosts != null) {
		val janePosts = postRepository.findByUserId(userByEmailForPosts.id!!)
		println("Jane's Posts (via user email lookup): $janePosts")
	}
	// Demonstrate custom raw SQL query
	println(
		"""
--- Custom SQL Query: Users older than 26 ---
"""
	)
	val customQueryResults = userRepository.findUsersOlderThan(27)
	println("Custom Query Result: $customQueryResults")
}