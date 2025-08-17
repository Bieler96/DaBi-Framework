package auth

import server.annotations.Body
import server.annotations.Controller
import server.annotations.PostMapping

@Controller("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    suspend fun register(@Body request: RegisterRequest): User {
        return authService.register(request)
    }

    @PostMapping("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse? {
        return authService.login(request)
    }
}
