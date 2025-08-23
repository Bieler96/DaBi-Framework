package auth

import dbdata.CrudRepository
import dbdata.Entity
import dbdata.Repository
import java.time.Instant

data class User(
    override val id: Int? = null,
    val email: String,
    val firstName: String,
    val lastName: String,
    val hash: String,
    val salt: String,
    val blocked: Boolean,
    override var createdAt: Instant? = null,
    override var updatedAt: Instant? = null,
    override var createdBy: Int? = null,
    override var updatedBy: Int? = null
) : Entity<Int>

@Repository
interface UserRepository : CrudRepository<User, Int> {
    suspend fun findByEmail(email: String): User?
}