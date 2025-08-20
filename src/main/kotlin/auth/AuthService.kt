package auth

import di.annotations.Injectable
import security.hashing.HashingService
import security.token.TokenClaim
import security.token.TokenConfig
import security.token.TokenService

data class RegisterRequest(val email: String, val password: String, val firstName: String, val lastName: String)
data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val token: String)

@Injectable
class AuthService(
    private val hashingService: HashingService,
    private val tokenService: TokenService,
    private val userRepository: UserRepository,
    private val tokenConfig: TokenConfig
) {
    suspend fun register(request: RegisterRequest): User {
        val saltedHash = hashingService.generateSaltedHash(request.password)
        val user = User(
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            hash = saltedHash.hash,
            salt = saltedHash.salt,
            blocked = false
        )
        return userRepository.save(user)
    }

    suspend fun login(request: LoginRequest): LoginResponse? {
        val user = userRepository.findByEmail(request.email) ?: return null

        if (user.blocked) {
            return null
        }

        val isValidPassword = hashingService.verify(
            request.password,
            security.hashing.SaltedHash(user.salt, user.hash)
        )

        if (!isValidPassword) {
            return null
        }

        val token = tokenService.generateToken(
            tokenConfig,
            TokenClaim("userId", user.id.toString()),
            TokenClaim("email", user.email)
        )

        return LoginResponse(token)
    }

    suspend fun findUserById(id: String): User? {
        return userRepository.findById(id.toInt())
    }
}
