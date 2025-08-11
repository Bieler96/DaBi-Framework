# Feature To-Do for DaBi-Framework (dbdata module)

This document outlines potential feature enhancements for the `dbdata` module, based on current capabilities and common ORM/repository patterns.

## 1. Extended Query Operators and Keywords (DONE)

*   **Sorting:**
    *   Implement `OrderBy` functionality (e.g., `findByNameOrderByNameDesc`, `findByAgeOrderByAgeAsc`).
    *   Allow multiple sorting criteria.
*   **Pagination:**
    *   Introduce `Limit` and `Offset` for queries (e.g., `findByNameLimit10Offset5`).
    *   Consider a `Pageable` object as a method parameter for more structured pagination.
*   **Distinct:**
    *   Add support for `findDistinctBy...` methods (e.g., `findDistinctNames`).
*   **Negation:**
    *   Implement `Not` operator (e.g., `findByNameNot`).
*   **Collection/String Checks:**
    *   `IsEmpty`/`IsNotEmpty` for collection or string fields.
*   **Boolean Field Operators:**
    *   `True`/`False` for boolean properties.

## 2. Relationship and Join Handling

*   **Explicit Relationship Definition:**
    *   Introduce annotations (e.g., `@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`) on entity properties to define relationships.
    *   This would allow the `DataProvider` to understand and manage joins more effectively.
*   **Eager/Lazy Loading:**
    *   Provide configurable strategies for loading related entities (e.g., `fetch = FetchType.LAZY` or `EAGER`).
*   **Complex Joins:**
    *   Enhance `ExposedDataProvider` to handle more complex join scenarios, including explicit join conditions and multiple joins in a single query.

## 3. Custom Query Implementation (`@Query` Annotation) (DONE)

*   **ExposedDataProvider Custom Queries:**
    *   Implement the `executeCustomQuery` method in `ExposedDataProvider` to allow raw SQL queries.
    *   Support for named parameters within custom SQL queries (e.g., `SELECT * FROM users WHERE age > :minAge`).
*   **MongoDataProvider Custom Queries:**
    *   Implement the `executeCustomQuery` method in `MongoDataProvider` for raw MongoDB queries (e.g., using JSON query strings or a custom DSL).

## 4. Pagination and Sorting API (DONE)

*   **Structured Parameters:**
    *   Introduce dedicated `Pageable` and `Sort` objects that can be passed as parameters to repository methods (e.g., `findAll(pageable: Pageable)`, `findByName(name: String, sort: Sort)`).

## 5. Optimized Batch Operations

*   **Database-Specific Batching:**
    *   Investigate and implement more efficient, database-specific batch insert, update, and delete operations for `saveAll` and `deleteAllInBatch` to improve performance for large datasets.

## 6. Integration with Validation Framework

*   **Automatic Entity Validation:**
    *   Integrate with the existing `validation` package to automatically validate entities before they are saved or updated.
    *   Allow configuration of validation groups or rules.

## 7. Additional Data Providers

*   **New Database Support:**
    *   Develop `DataProvider` implementations for other popular databases or data stores (e.g., Redis, Cassandra, Neo4j, JDBC directly without Exposed).

## 8. Entity Lifecycle Callbacks/Events

*   **Lifecycle Annotations:**
    *   Introduce annotations like `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostUpdate`, `@PreRemove`, `@PostRemove`, `@PostLoad` on entity methods.
    *   These methods would be automatically invoked at specific points in the entity's lifecycle.

## 9. Auditing (DONE)

*   **Automatic Field Tracking:**
    *   Implement automatic tracking for common auditing fields such as `createdAt`, `updatedAt`, `createdBy`, and `updatedBy`.
    *   This could involve annotations or an interceptor pattern.

## 10. Transaction Management API

*   **Explicit Transaction Control:**
    *   While Exposed handles transactions, a more explicit API at the repository or service layer could be beneficial for complex operations spanning multiple repositories or services.
    *   Consider `@Transactional` annotation or similar.

## 11. Enhanced Error Handling

*   **Specific Exception Types:**
    *   Define and throw more specific exception types for data access errors (e.g., `EntityNotFoundException`, `ConstraintViolationException`, `DataIntegrityViolationException`) to allow for more granular error handling in the application.
