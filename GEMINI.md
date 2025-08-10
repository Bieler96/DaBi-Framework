# DaBi-Framework

## Project Overview

The `DaBi-Framework` is a Kotlin JVM project built with Gradle. It appears to be a custom framework providing functionalities for:

*   **Logging:** A custom logging system with different appenders (Console, File, HTTP, JSON File) and log levels.
*   **Schema Validation:** A Domain Specific Language (DSL) for defining and validating data schemas, supporting various types like strings, numbers, booleans, arrays, and objects.

The project uses `org.json:json` as a dependency, suggesting it might handle JSON data processing, especially in the context of logging (JsonFileAppender) and schema validation.

## Building and Running

This project uses Gradle as its build automation tool.

*   **Build the project:**
    ```bash
    ./gradlew build
    ```

*   **Run tests:**
    ```bash
    ./gradlew test
    ```

*   **Clean the build directory:**
    ```bash
    ./gradlew clean
    ```

## Development Conventions

*   **Language:** Kotlin
*   **Build System:** Gradle
*   **Testing Framework:** JUnit Platform
*   **Code Structure:** Source code is organized under `src/main/kotlin` and `src/test/kotlin`.
