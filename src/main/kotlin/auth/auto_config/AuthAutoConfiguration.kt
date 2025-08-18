package auth.auto_config

import auth.AuthService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import di.DI
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import security.token.provideTokenConfig

fun Application.configureAuthentication() {
    val tokenConfig = provideTokenConfig(environment)
    val authService = DI.get<AuthService>()

    install(Authentication) {
        jwt {
            realm = tokenConfig.realm
            verifier(JWT
                .require(Algorithm.HMAC256(tokenConfig.secret))
                .withAudience(tokenConfig.audience)
                .withIssuer(tokenConfig.issuer)
                .build())
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