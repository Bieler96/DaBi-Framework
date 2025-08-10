package validation.error

sealed class ValidationError(message: String) : RuntimeException(message)