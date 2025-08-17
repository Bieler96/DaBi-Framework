package security.token

data class TokenConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val expiresIn: Long
)
