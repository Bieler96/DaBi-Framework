# DaBi-Framework Authentication Module Guide

This guide explains how to use the authentication module of the DaBi-Framework in your own project.

## 1. Introduction

The authentication module provides a simple and flexible way to add user authentication to your Ktor application. It includes features like user registration, login, password hashing, and JWT generation.

## 2. Installation

To use the authentication module, you need to add the DaBi-Framework as a dependency to your `build.gradle.kts` file. 

```kotlin
dependencies {
    implementation("com.github.Bieler96.DaBi-Framework:dabi-framework:v1.0.5")
}
```

## 3. Configuration

To use the authentication module, you need to configure the `AuthService` and its dependencies. This is typically done in the `Application.module()` function of your Ktor application.

### Exposed Configuration

```kotlin
import auth.AuthController
import auth.AuthService
import auth.UserRepository
import dbdata.DataRepositoryConfiguration
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import security.hashing.HashingService
import security.hashing.SHA256HashingService
import security.token.JwtTokenService
import security.token.TokenService
import server.autoDiscoverRoutes

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    val dataRepositoryConfiguration = DataRepositoryConfiguration()
    val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")

    val usersTable = object : Table("users") {
        val id = integer("id").autoIncrement()
        val email = varchar("email", 100)
        val firstName = varchar("firstName", 100)
        val lastName = varchar("lastName", 100)
        val hash = varchar("hash", 255)
        val salt = varchar("salt", 255)
        val blocked = bool("blocked")
        val createdAt = long("created_at").nullable()
        val updatedAt = long("updated_at").nullable()
        val createdBy = varchar("created_by", 255).nullable()
        val updatedBy = varchar("updated_by", 255).nullable()
        override val primaryKey = PrimaryKey(id)
    }

    transaction(database) {
        SchemaUtils.create(usersTable)
    }

    dataRepositoryConfiguration.registerExposedRepository(
        UserRepository::class,
        usersTable,
        auth.User::class,
        usersTable.id,
        database
    )

    val userRepository = dataRepositoryConfiguration.getRepository(UserRepository::class)
    val hashingService: HashingService = SHA256HashingService()
    val tokenService: TokenService = JwtTokenService()
    val authService = AuthService(hashingService, tokenService, userRepository)
    val authController = AuthController(authService)

    autoDiscoverRoutes("auth", listOf(authController))
}
```

### MongoDB Configuration

```kotlin
import auth.AuthController
import auth.AuthService
import auth.UserRepository
import com.mongodb.kotlin.client.coroutine.MongoClient
import dbdata.DataRepositoryConfiguration
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import security.hashing.HashingService
import security.hashing.SHA256HashingService
import security.token.JwtTokenService
import security.token.TokenService
import server.autoDiscoverRoutes

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    val dataRepositoryConfiguration = DataRepositoryConfiguration()
    val mongoClient = MongoClient.create("mongodb://localhost:27017")
    val database = mongoClient.getDatabase("test")
    val userCollection = database.getCollection<auth.User>("users")

    dataRepositoryConfiguration.registerMongoRepository(
        UserRepository::class,
        userCollection,
        auth.User::class
    )

    val userRepository = dataRepositoryConfiguration.getRepository(UserRepository::class)
    val hashingService: HashingService = SHA256HashingService()
    val tokenService: TokenService = JwtTokenService()
    val authService = AuthService(hashingService, tokenService, userRepository)
    val authController = AuthController(authService)

    autoDiscoverRoutes("auth", listOf(authController))
}
```

## 4. Usage

Once the authentication module is configured, you can use the `AuthController` to register and log in users. The controller exposes the following endpoints:

*   `POST /auth/register`: Registers a new user. The request body should be a JSON object with `email`, `password`, `firstName` and `lastName` fields.
*   `POST /auth/login`: Logs in a user. The request body should be a JSON object with `email` and `password` fields. The response will be a JSON object with a `token` field.

## 5. Customization

The authentication module is designed to be customizable. You can easily replace the default implementations of the `HashingService`, `TokenService`, and `UserRepository` with your own implementations.

### Using a Different Database

To use a different database, you need to provide your own implementation of the `UserRepository` interface. You can then register your repository with the `DataRepositoryConfiguration`.

### Using a Different Token Service

To use a different token service, you need to provide your own implementation of the `TokenService` interface. You can then pass your implementation to the `AuthService` constructor.

### Using a Different Hashing Service

To use a different hashing service, you need to provide your own implementation of the `HashingService` interface. You can then pass your implementation to the `AuthService` constructor.