package auth

import dbdata.CrudRepository
import dbdata.Entity
import dbdata.Repository
import java.time.LocalDateTime

data class User(
    override val id: Int? = null,
    val email: String,
    val firstName: String,
    val lastName: String,
    val hash: String,
    val salt: String,
    val blocked: Boolean,
    override var createdAt: LocalDateTime? = null,
    override var updatedAt: LocalDateTime? = null,
    override var createdBy: String? = null,
    override var updatedBy: String? = null
) : Entity<Int>

@Repository
interface UserRepository : CrudRepository<User, Int> {
    suspend fun findByEmail(email: String): User?
}