package auth.auto_config

import auth.AuthService
import di.DI
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import security.token.TokenService

fun Application.configureAuthentication() {
    val tokenService by DI.inject<TokenService>()
    val authService by DI.inject<AuthService>()

    install(Authentication) {
        jwt {
            realm = tokenService.getRealm()
            verifier(tokenService.getVerifier())
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (userId != null) {
                    authService.findUserById(userId)
                } else {
                    null
                }
            }
        }
    }
}