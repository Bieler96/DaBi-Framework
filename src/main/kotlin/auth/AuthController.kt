package auth

import dabiserverextension.Controller
import dabiserverextension.PostMapping
import dabiserverextension.RequestBody

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
