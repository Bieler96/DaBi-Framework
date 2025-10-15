package dbdata

import dbdata.migration.DatabaseMigrator
import dbdata.query.PageRequest
import dbdata.query.Sort
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import java.time.Instant

data class UserDto(val name: String, val email: String)

class User(
	override val id: Long? = null,
	val name: String,
	val email: String,
	val age: Int,
	val active: Boolean = true,
	override var createdAt: Instant? = null,
	override var updatedAt: Instant? = null,
	override var createdBy: Long? = null,
	override var updatedBy: Long? = null,
	val phoneNumber: String? = null
) : Entity<Long> {
	@OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
	var posts: List<Post> = emptyList()

	override fun toString(): String {
		return "User(id=$id, name='$name', email='$email', age=$age, active=$active, createdAt=$createdAt, updatedAt=$updatedAt, createdBy=$createdBy, updatedBy=$updatedBy, posts=${posts.map { it.id }})"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as User
		if (id != other.id) return false
		return true
	}

	override fun hashCode(): Int {
		return id?.hashCode() ?: 0
	}
}

class Post(
	override val id: Long? = null,
	val title: String,
	val content: String,
	val userId: Long,
	override var createdAt: Instant? = null,
	override var updatedAt: Instant? = null,
	override var createdBy: Long? = null,
	override var updatedBy: Long? = null
) : Entity<Long> {
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	var user: User? = null

	override fun toString(): String {
		return "Post(id=$id, title='$title', content='$content', userId=$userId, user=${user?.id}, createdAt=$createdAt, updatedAt=$updatedAt, createdBy=$createdBy, updatedBy=$updatedBy)"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as Post
		if (id != other.id) return false
		return true
	}

	override fun hashCode(): Int {
		return id?.hashCode() ?: 0
	}
}

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

	// Extended Query Examples
	suspend fun findByAgeOrderByNameDesc(age: Int): List<User>
	suspend fun findByNameLimit1(name: String): List<User>
	suspend fun findDistinctByAge(age: Int): List<User>
	suspend fun findByNameNot(name: String): List<User>
	suspend fun findByEmailIsNotEmpty(): List<User>
	suspend fun findByActiveTrue(): List<User>

	suspend fun findByNameAsUserDto(name: String): List<UserDto>

	@Query("SELECT * FROM users WHERE age > :minAge")
	suspend fun findUsersOlderThan(minAge: Int): List<User>

	// Aggregation Queries
	suspend fun sumAge(): Number?
	suspend fun avgAge(): Number?
	suspend fun minAge(): Int?
	suspend fun maxAge(): Int?
	suspend fun countUsersByAge(): Map<Int, Long>
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
	val active = bool("active").default(true)
	val createdAt = timestamp("created_at").nullable()
	val updatedAt = timestamp("updated_at").nullable()
	val createdBy = long("created_by").nullable()
	val updatedBy = long("updated_by").nullable()
	val phoneNumber = varchar("phone_number", 255).nullable()

	override val primaryKey = PrimaryKey(id)
}

object PostsTable : Table("posts") {
	val id = long("id").autoIncrement()
	val title = varchar("title", 255)
	val content = text("content")
	val userId = long("user_id").references(UsersTable.id)
	val createdAt = timestamp("created_at").nullable()
	val updatedAt = timestamp("updated_at").nullable()
	val createdBy = long("created_by").nullable()
	val updatedBy = long("updated_by").nullable()
	override val primaryKey = PrimaryKey(id)
}

fun setupRepositories(): DataRepositoryConfiguration {
	val config = DataRepositoryConfiguration()

	// Exposed Setup
	val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

	// Run database migrations
	val migrator = DatabaseMigrator(database)
	migrator.migrate()

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

	val user1 = userRepository.save(User(name = "John Doe", email = "john@example.com", age = 30, active = true))
	val user2 = userRepository.save(User(name = "Jane Smith", email = "jane@example.com", age = 25, active = false))
	val user3 = userRepository.save(User(name = "John Smith", email = "john.smith@example.com", age = 30, active = true))

	println("Saved User1: $user1")
	println("Saved User2: $user2")
	println("Saved User3: $user3")

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
---	Paginated Users (Page 0, Size 2) ---
"""
	)
	val page0 = userRepository.findAll(PageRequest(pageNumber = 0, pageSize = 2))
	println("Page 0: $page0")

	println(
		"""
---	Paginated Users (Page 1, Size 2) ---
"""
	)
	val page1 = userRepository.findAll(PageRequest(pageNumber = 1, pageSize = 2))
	println("Page 1: $page1")

	// Demonstrate sorting
	println(
		"""
---	Users sorted by Name ASC ---
"""
	)
	val sortedByNameAsc = userRepository.findAll(Sort(property = "name", direction = Sort.Direction.ASC))
	println("Sorted by Name ASC: $sortedByNameAsc")

	println(
		"""
---	Users sorted by Age DESC ---
"""
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
---	Custom SQL Query: Users older than 26 ---
"""
	)
	val customQueryResults = userRepository.findUsersOlderThan(27)
	println("Custom Query Result: $customQueryResults")

	println(
		"""
---	Extended Query Examples ---
"""
	)

	// OrderBy
	val usersByAgeSorted = userRepository.findByAgeOrderByNameDesc(30)
	println("Users with age 30, sorted by name desc: $usersByAgeSorted")

	// Limit
	val limitedUser = userRepository.findByNameLimit1("John Doe")
	println("Limited user: $limitedUser")

	// Distinct
	val distinctUsersByAge = userRepository.findDistinctByAge(30)
	println("Distinct users by age 30: $distinctUsersByAge")

	// Not
	val usersNotJohnDoe = userRepository.findByNameNot("John Doe")
	println("Users not named John Doe: $usersNotJohnDoe")

	// IsNotEmpty
	val usersWithEmail = userRepository.findByEmailIsNotEmpty()
	println("Users with non-empty email: $usersWithEmail")

	// True
	val activeUsers = userRepository.findByActiveTrue()
	println("Active users: $activeUsers")

	// Demonstrate fetching a post and its related user
	println("\n--- Fetching post with eager-loaded user ---")
	val postWithUser = postRepository.findById(post1.id!!)
	println("Fetched Post: $postWithUser")
	println("User from Post: ${postWithUser?.user}")

	// Demonstrate fetching a user and their related posts
	println("\n--- Fetching user with eager-loaded posts ---")
	val userWithPosts = userRepository.findById(user1.id!!)
	println("Fetched User: $userWithPosts")
	println("Posts from User: ${userWithPosts?.posts}")

	// --- Test Projection ---
	println("\n--- Testing Projection ---")
	val userDtos = userRepository.findByNameAsUserDto("John Doe")
	println("User DTOs for 'John Doe': $userDtos")

	// --- Test Aggregation ---
	println("\n--- Testing Aggregation ---")
	val totalAge = userRepository.sumAge()
	println("Sum of all ages: $totalAge")

	val averageAge = userRepository.avgAge()
	println("Average age: $averageAge")

	val minAge = userRepository.minAge()
	println("Minimum age: $minAge")

	val maxAge = userRepository.maxAge()
	println("Maximum age: $maxAge")

	val usersGroupedByAge = userRepository.countUsersByAge()
	println("Users grouped by age: $usersGroupedByAge")
}