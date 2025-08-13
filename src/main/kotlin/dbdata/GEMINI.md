# GEMINI Code Guide

This document provides a comprehensive overview of the DaBi-Framework's data access layer, designed to help you understand its structure, conventions, and how to contribute effectively.

## Project Overview

This project is a sophisticated data access framework written in Kotlin. It simplifies database interactions by providing a repository pattern that abstracts the underlying data source. The framework is designed to be extensible, with current support for SQL databases via JetBrains Exposed and NoSQL databases via MongoDB.

### Core Concepts

*   **`CrudRepository`:** A generic interface for basic CRUD (Create, Read, Update, Delete) operations.
*   **`RepositoryProxyFactory`:** Creates dynamic proxy implementations of repository interfaces at runtime.
*   **`DataProvider`:** An abstraction for the underlying data storage. The framework includes two implementations:
    *   `ExposedDataProvider`: For relational databases using the Exposed framework.
    *   `MongoDataProvider`: For MongoDB.
*   **Query Derivation:** The framework automatically generates database queries from method names in your repository interfaces. For example, a method named `findByNameAndAge(name: String, age: Int)` will automatically be translated into a query that finds records by name and age.

### Key Technologies

*   **Kotlin:** The primary programming language.
*   **JetBrains Exposed:** For SQL database access.
*   **MongoDB Kotlin Driver:** For MongoDB access.
*   **Java Reflection:** Used for the dynamic proxy creation.

## Building and Running

As this is a library project, the primary way to "run" it is by executing the example in `Main.kt`. This file sets up an in-memory H2 database, registers repositories, and demonstrates various features of the framework.

To run the example, execute the `main` function in `src/main/kotlin/dbdata/Main.kt`.

### Dependencies

This project uses Gradle for dependency management. The required dependencies (like Kotlin, Exposed, and the MongoDB driver) are not present in this directory, but would be defined in a `build.gradle.kts` file at the project root.

## Development Conventions

### Coding Style

*   The code follows standard Kotlin conventions.
*   Use clear and descriptive names for variables, functions, and classes.
*   Interfaces are used extensively to promote loose coupling.

### Repository Design

*   Repository interfaces should extend `CrudRepository<T, ID>`.
*   Custom query methods should follow the naming conventions understood by the `QueryMethodParser`. For example:
    *   `findBy<PropertyName>(value: Type)`
    *   `countBy<PropertyName>(value: Type)`
    *   `deleteBy<PropertyName>(value: Type)`
    *   `existsBy<PropertyName>(value: Type)`
*   Complex queries can be built by combining properties with `And` and `Or`. For example: `findByNameAndAge(name: String, age: Int)`.
*   For queries that cannot be expressed through method names, use the `@Query` annotation with a native query string.

### Custom Query Methods

Here is a table of all supported keywords for custom query methods:

| Keyword | Example | Description |
|---|---|---|
| `findDistinctBy` | `findDistinctByAge(age: Int)` | Finds distinct values for a given property. |
| `findBy` | `findByName(name: String)` | Finds records by a specific property. |
| `countBy` | `countByAge(age: Int)` | Counts records by a specific property. |
| `deleteBy` | `deleteByEmail(email: String)` | Deletes records by a specific property. |
| `existsBy` | `existsByEmail(email: String)` | Checks if a record exists by a specific property. |
| `sum<Property>By` | `sumAge()` | Calculates the sum of a property. |
| `avg<Property>By` | `avgAge()` | Calculates the average of a property. |
| `min<Property>By` | `minAge()` | Finds the minimum value of a property. |
| `max<Property>By` | `maxAge()` | Finds the maximum value of a property. |
| `...OrderBy<Property>Asc` | `findByAgeOrderByNameAsc(age: Int)` | Orders the results by a property in ascending order. |
| `...OrderBy<Property>Desc` | `findByAgeOrderByNameDesc(age: Int)` | Orders the results by a property in descending order. |
| `...Limit<Number>` | `findByNameLimit1(name: String)` | Limits the number of results. |
| `...Offset<Number>` | `findByNameOffset5(name: String)` | Skips a number of results. |
| `...<Property>GreaterThan` | `findByAgeGreaterThan(age: Int)` | Finds records where a property is greater than a value. |
| `...<Property>LessThan` | `findByAgeLessThan(age: Int)` | Finds records where a property is less than a value. |
| `...<Property>GreaterThanEqual` | `findByAgeGreaterThanEqual(age: Int)` | Finds records where a property is greater than or equal to a value. |
| `...<Property>LessThanEqual` | `findByAgeLessThanEqual(age: Int)` | Finds records where a property is less than or equal to a value. |
| `...<Property>Containing` | `findByEmailContaining(substring: String)` | Finds records where a string property contains a value. |
| `...<Property>ContainingIgnoreCase` | `findByEmailContainingIgnoreCase(substring: String)` | Finds records where a string property contains a value, ignoring case. |
| `...<Property>StartingWith` | `findByNameStartingWith(prefix: String)` | Finds records where a string property starts with a value. |
| `...<Property>EndingWith` | `findByNameEndingWith(suffix: String)` | Finds records where a string property ends with a value. |
| `...<Property>IsNull` | `findByNameIsNull()` | Finds records where a property is null. |
| `...<Property>IsNotNull` | `findByNameIsNotNull()` | Finds records where a property is not null. |
| `...<Property>In` | `findByAgeIn(ages: List<Int>)` | Finds records where a property is in a list of values. |
| `...<Property>NotIn` | `findByAgeNotIn(ages: List<Int>)` | Finds records where a property is not in a list of values. |
| `...<Property>Between` | `findByAgeBetween(minAge: Int, maxAge: Int)` | Finds records where a property is between two values. |
| `...<Property>Not` | `findByNameNot(name: String)` | Finds records where a property is not equal to a value. |
| `...<Property>IsEmpty` | `findByEmailIsEmpty()` | Finds records where a string property is empty. |
| `...<Property>IsNotEmpty` | `findByEmailIsNotEmpty()` | Finds records where a string property is not empty. |
| `...<Property>True` | `findByActiveTrue()` | Finds records where a boolean property is true. |
| `...<Property>False` | `findByActiveFalse()` | Finds records where a boolean property is false. |


### Data Provider Implementation

When adding support for a new data source, you must:

1.  Create a new class that extends `DataProvider<T, ID>`.
2.  Implement all the abstract methods in the `DataProvider` class.
3.  Update the `DataRepositoryConfiguration` class to allow registration of repositories with the new data provider.

### Testing

There are no tests in the current directory. However, a robust testing strategy would involve:

*   Unit tests for the `QueryMethodParser` to ensure correct parsing of method names.
*   Integration tests for each `DataProvider` implementation to verify database interactions.
*   End-to-end tests that use the repository proxies to perform operations against a real or in-memory database.