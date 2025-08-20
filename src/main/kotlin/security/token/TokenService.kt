package security.token

import di.annotations.Injectable

@Injectable
interface TokenService {
    fun generateToken(
        config: TokenConfig,
        vararg claims: TokenClaim
    ): String
}