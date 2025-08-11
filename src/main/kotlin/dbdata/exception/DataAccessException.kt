package dbdata.exception

/**
 * Base exception for data access errors.
 */
open class DataAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when an expected entity is not found in the database.
 */
class EntityNotFoundException(entityName: String, id: Any) : DataAccessException("Entity '$entityName' with id '$id' not found.")

/**
 * Thrown when a database constraint (e.g., unique key) is violated.
 */
class ConstraintViolationException(message: String, cause: Throwable? = null) : DataAccessException(message, cause)

/**
 * Thrown when an operation violates data integrity (e.g., foreign key constraint).
 */
class DataIntegrityViolationException(message: String, cause: Throwable? = null) : DataAccessException(message, cause)
