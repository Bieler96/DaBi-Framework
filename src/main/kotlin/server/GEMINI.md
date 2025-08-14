# Project Overview

This directory contains core server-side components for a Kotlin application, likely built with the Ktor framework. It features a custom auto-discovery mechanism for routing, allowing developers to define API endpoints using annotations (`@Controller`, `@GetMapping`, `@PostMapping`, etc.). Error handling is centralized, and data serialization/deserialization is handled using `kotlinx.serialization`.

Key technologies and patterns identified:
*   **Kotlin:** The primary programming language.
*   **Ktor:** A framework for building asynchronous servers and clients in Kotlin.
*   **Custom Auto-Discovery:** A custom implementation that scans for `@Controller` annotated classes and their methods to automatically register routes. This simplifies API endpoint definition.

    **Example Usage in Application Module:**
    ```kotlin
    package com.example.app

    import io.ktor.server.application.*
    import io.ktor.server.netty.*
    import server.autoDiscoverRoutes

    fun main(args: Array<String>): Unit = EngineMain.main(args)

    fun Application.module() {
        // ... other Ktor configurations
        autoDiscoverRoutes("com.example.controller") // Specify the base package to scan
    }
    ```
*   **Annotation-driven Development:** Extensive use of custom annotations (`@Controller`, `@GetMapping`, `@PostMapping`, `@PathParam`, `@QueryParam`, `@Body`) for defining routing and parameter handling.
*   **Centralized Error Handling:** `GlobalExceptionHandler.kt` provides a consistent way to handle exceptions across the application.

    **Example Usage in Application Module:**
    ```kotlin
    package com.example.app

    import io.ktor.server.application.*
    import server.installGlobalExceptionHandler

    fun Application.module() {
        // ... other Ktor configurations
        installGlobalExceptionHandler()
    }
    ```
*   **`kotlinx.serialization`:** Used for efficient and type-safe serialization and deserialization of data, as seen in `ErrorResponse.kt`.

# Building and Running

This project is likely built using Gradle.

**To build the project:**

```bash
./gradlew build
```

**To run the application:**

```bash
./gradlew run
```

**Note:** The exact Gradle tasks might vary depending on the project's `build.gradle` configuration. If these commands do not work, please provide the correct build and run commands for this project.

# Development Conventions

*   **Kotlin First:** The codebase is entirely in Kotlin, adhering to Kotlin's idiomatic practices.
*   **Ktor Framework:** Development follows Ktor's conventions for defining routes, handling requests, and managing application lifecycle.
*   **Annotation-Based Routing:** New API endpoints should be defined within classes annotated with `@Controller`, using the custom HTTP method annotations (`@GetMapping`, `@PostMapping`, etc.) for individual functions.

    **Example:**
    ```kotlin
    package com.example.controller

    import server.annotations.*
    import io.ktor.server.application.*
    import io.ktor.server.response.*

    @Controller("/api/v1/users")
    class UserController {

        @GetMapping("/{id}")
        suspend fun getUserById(call: ApplicationCall, @PathParam("id") id: String) {
            call.respond("User with ID: $id")
        }

        @PostMapping
        suspend fun createUser(call: ApplicationCall, @Body user: User) {
            call.respond("Creating user: ${user.name}")
        }
    }

    @Serializable
    data class User(val name: String, val email: String)
    ```
*   **Parameter Handling:** Parameters for controller methods are automatically injected based on annotations like `@PathParam`, `@QueryParam`, and `@Body`.

    **Example:**
    ```kotlin
    package com.example.controller

    import server.annotations.*
    import io.ktor.server.application.*
    import io.ktor.server.response.*

    @Controller("/api/v1/products")
    class ProductController {

        @GetMapping("/{productId}")
        suspend fun getProduct(
            call: ApplicationCall,
            @PathParam("productId") productId: String,
            @QueryParam("category") category: String?,
            @QueryParam("limit") limit: Int = 10
        ) {
            call.respond("Product ID: $productId, Category: $category, Limit: $limit")
        }

        @PostMapping
        suspend fun createProduct(call: ApplicationCall, @Body product: Product) {
            call.respond("Creating product: ${product.name}")
        }
    }

    @Serializable
    data class Product(val name: String, val price: Double)
    ```
*   **Error Handling:** Custom exceptions should be handled by extending the existing error handling in `GlobalExceptionHandler.kt` or by adding specific exception handlers within the Ktor `StatusPages` plugin.
*   **Serialization:** Data models intended for API communication should be marked with `@Serializable` and use `kotlinx.serialization` for JSON conversion.

    **Example:**
    ```kotlin
    package server.model

    import kotlinx.serialization.Serializable

    @Serializable
    data class ErrorResponse(val message: String?, val httpStatusCode: Int)
    ```
