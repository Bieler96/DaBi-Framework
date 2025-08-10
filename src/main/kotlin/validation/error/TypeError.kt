package validation.error

class TypeError(expected: String) : ValidationError("Expected $expected")