
# GEMINI.md

## Project Overview

This project is a data validation library written in Kotlin. It provides a fluent DSL (Domain Specific Language) for defining validation schemas for various data types. The library appears to be inspired by popular validation libraries like Zod.

The core components of the library are:

*   **Schemas:** Define the structure and validation rules for data. The library provides schemas for `String`, `Number`, `Boolean`, `Array`, and `Object` data types.
*   **DSL:** A simple and expressive DSL to create and combine schemas.
*   **Validation Rules:** A rich set of built-in validation rules for each data type (e.g., `min`, `max`, `email`, `uuid`, etc.).
*   **Error Handling:** A set of custom error classes (`ValidationError`, `TypeError`, `CheckError`) to represent different types of validation failures.

## Building and Running

There are no explicit build or run commands in the provided files. However, this is a standard Kotlin project, so it can be built and run using Gradle or Maven.

**TODO:** Add specific build and run commands once the build system is identified.

## Development Conventions

*   **Package Structure:** The code is organized into two main packages: `validation.error` for error classes and `validation.schema` for schema definitions. The `schema` package is further divided into `dsl` for the DSL and `feature` for the individual schema implementations.
*   **Immutability:** The schema classes seem to be designed to be immutable. Each validation rule returns a new instance of the schema, allowing for method chaining.
*   **Extensibility:** The `Schema` interface can be implemented to create custom schemas for new data types.
*   **Testing:** There are no tests in the provided files. It is recommended to add a comprehensive test suite to ensure the correctness of the validation logic.

## Key Files

*   `src/main/kotlin/validation/schema/dsl/DSL.kt`: This file contains the main DSL functions for creating validation schemas.
*   `src/main/kotlin/validation/schema/dsl/Schema.kt`: This file defines the base `Schema` interface that all schema classes implement.
*   `src/main/kotlin/validation/schema/feature/`: This directory contains the implementation of the different schema types (`StringSchema`, `NumberSchema`, etc.).
*   `src/main/kotlin/validation/error/`: This directory contains the different error types used by the validation library.
