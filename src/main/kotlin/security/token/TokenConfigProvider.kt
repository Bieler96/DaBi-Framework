package security.token

import io.ktor.server.application.*

fun provideTokenConfig(environment: ApplicationEnvironment): TokenConfig {
    val config = environment.config
    val secret = config.property("ktor.security.token.secret").getString()
    val issuer = config.property("ktor.security.token.issuer").getString()
    val audience = config.property("ktor.security.token.audience").getString()
    val expiresIn = config.property("ktor.security.token.expiresIn").getString().toLong()
    val realm = config.property("ktor.security.token.realm").getString()
    return TokenConfig(issuer, audience, secret, expiresIn, realm)
}
