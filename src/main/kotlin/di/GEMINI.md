# Dependency Injection (DI) Framework

This document provides a comprehensive guide to the Dependency Injection (DI) framework within this project. It's a lightweight, annotation-based framework designed for managing dependencies in a clean and decoupled way.

## Core Concepts

The DI framework is built around a few key components:

- **`Container`**: The heart of the DI system. It manages the lifecycle of objects (called "dependencies" or "services"), including their creation, storage, and resolution.
- **`@Injectable`**: An annotation that marks a class as eligible for automatic dependency injection. The container can automatically detect, instantiate, and manage classes marked with `@Injectable`.
- **`@Inject`**: An annotation used to specify which constructor should be used for instantiation if the primary constructor is not suitable.

## 1. Getting Started: Basic Usage

### Step 1: Mark Your Classes as `@Injectable`

To make a class available for injection, simply annotate it with `@Injectable`.

**Example:**

```kotlin
// In services/MyService.kt

@Injectable
class MyService {
    fun doSomething() {
        println("Service is doing something.")
    }
}
```

### Step 2: Inject and Retrieve Your Dependencies

Use the global `DI` object to get an instance of your service. The container handles the instantiation for you.

**Example:**

```kotlin
// In your application's entry point or another class

fun main() {
    // The container will create an instance of MyService automatically
    val myService = DI.get<MyService>()
    myService.doSomething() // Output: Service is doing something.
}
```

## 2. Constructor Injection

The framework automatically resolves dependencies required by a class's constructor.

### How it Works

When `DI.get<T>()` is called for a class `T`, the container does the following:
1. It looks for the **primary constructor** of class `T`.
2. If no primary constructor exists, it looks for a constructor explicitly marked with **`@Inject`**.
3. It then examines the constructor's parameters. For each parameter, it recursively fetches the corresponding dependency from the container.
4. Once all constructor parameters are resolved, it creates an instance of `T` and returns it.

**Example:**

```kotlin
@Injectable
class AnotherService {
    fun provideData() = "Some important data"
}

@Injectable
class MyController(private val service: AnotherService) {
    fun showData() {
        println(service.provideData())
    }
}

fun main() {
    // 1. You ask for MyController.
    // 2. The container sees MyController needs an AnotherService.
    // 3. It gets/creates an instance of AnotherService.
    // 4. It uses that instance to construct MyController.
    val controller = DI.get<MyController>()
    controller.showData() // Output: Some important data
}
```

> **Note:** All dependencies required by a constructor must also be registered in the container (e.g., by being marked `@Injectable`).

## 3. Manual Registration

For cases where automatic instantiation is not suitable (e.g., for classes from external libraries or for objects requiring complex setup), you can register instances manually.

**Example:**

```kotlin
// A class from an external library
class ExternalDatabaseClient(connectionString: String) {
    // ...
}

fun main() {
    // Create the instance yourself
    val dbClient = ExternalDatabaseClient("user:password@host:port/db")

    // Register it with the container
    DI.register(ExternalDatabaseClient::class, dbClient)

    // Now it can be injected elsewhere
    val myService = DI.get<MyServiceUsingDatabase>()
}

@Injectable
class MyServiceUsingDatabase(private val dbClient: ExternalDatabaseClient) {
    // ...
}

```

## 4. Configuration with the DSL

The framework includes a powerful Domain-Specific Language (DSL) for configuring dependencies in a centralized and readable way. This is useful for managing scopes (like singletons vs. factories) or complex object graphs.

### The `dependencies` Block

Use the `dependencies { ... }` block to define how objects should be created and managed.

```kotlin
DI.configure {
    // DSL configurations go here
}
```

### Singleton vs. Factory

- **`singleton`**: A single instance of the class is created and shared throughout the application. Every call to `DI.get()` for this type will return the exact same object.
- **`factory`**: A new instance is created every time `DI.get()` is called.

**Example of DSL Configuration:**

```kotlin
interface FileUploader
class S3Uploader(config: S3Config) : FileUploader
class LocalUploader : FileUploader

class S3Config(val bucket: String)

fun main() {
    DI.configure {
        // Register S3Config as a singleton
        singleton { S3Config("my-app-bucket") }

        // Register S3Uploader as a singleton that depends on S3Config
        // The container will automatically provide S3Config from the line above
        singleton<FileUploader> { S3Uploader(DI.get()) }

        // If you wanted a new uploader every time, you could use a factory
        // factory<FileUploader> { S3Uploader(DI.get()) }
    }

    // Retrieve the configured dependency
    val uploader = DI.get<FileUploader>() // This will be an S3Uploader instance
}
```
This DSL provides a clean way to bind interfaces to concrete implementations and manage their lifecycles.

## 5. Advanced: Component Scope DSL

The `dsl/ComponentScopeDsl.kt` file introduces a DSL for defining scopes and components (`defineComponents { ... }`). This is intended for organizing dependencies into modules or features.

**Example:**

```kotlin
defineComponents {
    component("Authentication") {
        singleton()
        dependsOn("Database")
    }
    component("Database") {
        singleton()
    }
}
```

This feature appears to be for describing the architecture and relationships between high-level components. At present, the configuration from this DSL is not automatically integrated into the main DI container. Its primary use is for documentation and architectural validation rather than direct dependency provisioning.
