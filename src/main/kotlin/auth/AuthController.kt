package auth

import server.Controller
import server.PostMapping
import server.RequestBody

@Controller("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    suspend fun register(@RequestBody request: RegisterRequest): User {
        return authService.register(request)
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequest): LoginResponse? {
        return authService.login(request)
    }
}
