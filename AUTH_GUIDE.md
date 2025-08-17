# DaBi-Framework Authentication Module Guide

This guide explains how to use the authentication module of the DaBi-Framework in your own project.

## 1. Introduction

The authentication module provides a simple and flexible way to add user authentication to your Ktor application. It includes features like user registration, login, password hashing, and JWT generation. With the latest updates, the configuration is now mostly automatic.

## 2. Installation

To use the authentication module, you need to add the DaBi-Framework as a dependency to your `build.gradle.kts` file.

```kotlin
dependencies {
    implementation("com.github.Bieler96.DaBi-Framework:dabi-framework:v1.0.5")
}
```

## 3. Configuration

The framework handles most of the authentication setup automatically. When you call `autoDiscoverRoutes`, it will also configure Ktor's `Authentication` feature with a JWT provider.

### Minimal Configuration

Here is a minimal example of a Ktor `Application.module()`:

```kotlin
import di.DI
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import server.autoDiscoverRoutes

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    // Initialize your dependencies into the DI container
    // e.g., DI.bind<MyInterface>(MyImplementation())
    
    // This will automatically configure authentication and discover your controllers.
    autoDiscoverRoutes("com.yourapp.package") 
}
```

### Token Configuration

The `TokenService` is configured through your Ktor `application.conf` file. The framework will automatically pick up these values. The relevant keys are defined in the `TokenConfig` class.

Example `application.conf`:

```hocon
ktor {
    security {
        token {
            audience = "my-audience"
            issuer = "my-issuer"
            realm = "my-app"
            secret = "my-super-secret-for-jwt"
            expiresIn = 3600000
        }
    }
}
```

### Dependencies

The automatic configuration relies on a Dependency Injection (DI) container. You need to make sure that your implementations of `AuthService`, `TokenService`, and `HashingService` are bound in the DI container.

## 4. Usage

The `AuthController` provides the public endpoints for registration and login:

*   `POST /auth/register`: Registers a new user. The request body should be a JSON object with `email`, `password`, `firstName` and `lastName` fields.
*   `POST /auth/login`: Logs in a user. The request body should be a JSON object with `email` and `password` fields. The response will be a JSON object with a `token` field.

### Protecting Routes

To protect a route and require authentication, simply add the `@Authenticated` annotation to your controller method.

```kotlin
import server.annotations.GetMapping
import server.annotations.Controller
import auth.auto_config.Authenticated

@Controller("/my-protected-resource")
class MyController {

    @Authenticated
    @GetMapping
    fun getProtectedData(): String {
        // This code will only be executed if the user is authenticated.
        // You can access the user principal via the call object.
        return "This is some protected data."
    }
}
```

When a route is marked with `@Authenticated`, the framework ensures that only requests with a valid JWT can access it.

## 5. Customization

The authentication module is designed to be customizable. You can easily replace the default implementations of the `HashingService`, `TokenService`, and `UserRepository` with your own implementations by binding them in the DI container.

### Using a Different Database

To use a different database, you need to provide your own implementation of the `UserRepository` interface and bind it in the DI container.

### Using a Different Token Service

To use a different token service, you need to provide your own implementation of the `TokenService` interface and bind it in the DI container.

### Using a Different Hashing Service

To use a different hashing service, you need to provide your own implementation of the `HashingService` interface and bind it in the DI container.
