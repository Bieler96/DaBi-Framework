package validation.schema.feature

import validation.error.CheckError
import validation.error.TypeError
import validation.schema.dsl.Schema
import java.net.MalformedURLException
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.*

class StringSchema : Schema<String> {
	private val checks = mutableListOf<(String) -> Unit>()
	private var isOptional = false
	private var isNullable = false
	private var defaultValue: String? = null

	//###########################//
	// Validation
	//###########################//

	/**
	 * Checks if the string has a minimum length.
	 * @param length The minimum length to check for.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is shorter than the minimum length.
	 */
	fun min(length: Int): StringSchema {
		checks.add { if (it.length < length) throw CheckError("String too short") }
		return this
	}

	/**
	 * Checks if the string has a maximum length.
	 * @param length The maximum length to check for.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string exceeds the maximum length.
	 */
	fun max(length: Int): StringSchema {
		checks.add { if (it.length > length) throw CheckError("String too long") }
		return this
	}

	/**
	 * Checks if the string has the exact length.
	 * @param length The exact length to check for.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string does not have the exact length.
	 */
	fun length(length: Int): StringSchema {
		checks.add { if (it.length != length) throw CheckError("String has incorrect length") }
		return this
	}

	/**
	 * Checks if the string is a valid UUID.
	 * A valid UUID is defined as one that can be parsed into a UUID object.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid UUID.
	 */
	fun uuid(): StringSchema {
		checks.add {
			try {
				UUID.fromString(it)
			} catch (e: IllegalArgumentException) {
				throw CheckError("String is not a valid UUID")
			}
		}
		return this
	}

	/**
	 * Checks if the string includes the given substring.
	 * @param substring The substring to check for.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string does not include the substring.
	 */
	fun includes(substring: String): StringSchema {
		checks.add { if (!it.contains(substring)) throw CheckError("String does not include substring") }
		return this
	}

	/**
	 * Checks if the string starts with the given prefix.
	 * @param prefix The prefix to check for.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string does not start with the prefix.
	 */
	fun startsWith(prefix: String): StringSchema {
		checks.add { if (!it.startsWith(prefix)) throw CheckError("String does not start with prefix") }
		return this
	}

	/**
	 * Checks if the string ends with the given suffix.
	 * @param suffix The suffix to check for.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string does not end with the suffix.
	 */
	fun endsWith(suffix: String): StringSchema {
		checks.add { if (!it.endsWith(suffix)) throw CheckError("String does not end with suffix") }
		return this
	}

	/**
	 * Checks if the string is a valid datetime in the format YYYY-MM-DDTHH:mm:ssZ.
	 * A valid datetime is defined as one that can be parsed into an OffsetDateTime object.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid datetime.
	 */
	fun datetime(): StringSchema {
		checks.add {
			try {
				OffsetDateTime.parse(it)
			} catch (e: DateTimeParseException) {
				throw CheckError("String is not a valid datetime")
			}
		}
		return this
	}

	/**
	 * Checks if the string is a valid date in the format YYYY-MM-DD.
	 * A valid date is defined as one that matches the regex pattern.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid date.
	 */
	fun date(): StringSchema {
		checks.add {
			val regex = """^\d{4}-\d{2}-\d{2}$""".toRegex()
			if (!regex.matches(it)) throw CheckError("String is not a valid date")
		}
		return this
	}

	/**
	 * Checks if the string is a valid time in the format HH:mm:ss or HH:mm:ss.SSSSSS.
	 * A valid time is defined as one that matches the regex pattern.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid time.
	 */
	fun time(): StringSchema {
		checks.add {
			val regex = """^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)(\.\d{1,6})?$""".toRegex()
			if (!regex.matches(it)) throw CheckError("String is not a valid time")
		}
		return this
	}

	/**
	 * Checks if the string is a valid IPv4 or IPv6 address.
	 * A valid IP address is defined as one that matches the IPv4 or IPv6 regex pattern.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid IP address.
	 */
	fun ip(): StringSchema {
		checks.add {
			val ipV4Pattern =
				"^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])$"
			val ipV4Regex = ipV4Pattern.toRegex()

			val ipV6Pattern = """
				^(?:(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,7}:|(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}|(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}|(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}|(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:(?:(?::[0-9a-fA-F]{1,4}){1,6})|:(?:(?::[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(?::[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(?:ffff(?::0{1,4})?:)?(?:(?:25[0-5]|(?:2[0-4]|1?[0-9])?[0-9])\.){3}(?:25[0-5]|(?:2[0-4]|1?[0-9])?[0-9])|(?:[0-9a-fA-F]{1,4}:){1,4}:(?:(?:25[0-5]|(?:2[0-4]|1?[0-9])?[0-9])\.){3}(?:25[0-5]|(?:2[0-4]|1?[0-9])?[0-9]))$"
			""".trimIndent()
			val ipV6Regex = ipV6Pattern.toRegex()

			if (!ipV4Regex.matches(it) && !ipV6Regex.matches(it)) throw CheckError("String is not a valid IP address")
		}
		return this
	}

	/**
	 * Checks if the string is a valid IPv4 address.
	 * A valid IPv4 address is defined as one that matches the IPv4 regex pattern.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid IPv4 address.
	 */
	fun ipV4(): StringSchema {
		checks.add {
			val ipV4Pattern =
				"^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])$"
			val ipV4Regex = ipV4Pattern.toRegex()

			if (!ipV4Regex.matches(it)) throw CheckError("String is not a valid IPv4 address")
		}
		return this
	}

	/**
	 * Checks if the string is a valid IPv6 address.
	 * A valid IPv6 address is defined as one that matches the IPv6 regex pattern.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid IPv6 address.
	 */
	fun ipV6(): StringSchema {
		checks.add {
			val ipV6Pattern = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\$"
			val ipV6Regex = ipV6Pattern.toRegex()

			if (!ipV6Regex.matches(it)) throw CheckError("String is not a valid IPv6 address")
		}
		return this
	}

	/**
	 * Checks if the string is a valid URL.
	 * A valid URL is defined as one that can be parsed into a URI and then converted to a URL.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid URL.
	 */
	fun url(): StringSchema {
		checks.add {
			try {
				val uri: URI = URI(it)
				uri.toURL()
			} catch (e: IllegalArgumentException) {
				throw CheckError("String is not a valid URL: If this URL is not absolute")
			} catch (e: MalformedURLException) {
				throw CheckError("String is not a valid URL: If a protocol handler for the URL could not be found, or if some other error occurred while constructing the URL")
			}
		}
		return this
	}

	/**
	 * Checks if the string is a valid email address.
	 * A valid email address is defined as one that contains an '@' symbol and a '.' after the '@'.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid email address.
	 */
	fun email(): StringSchema {
		checks.add {
			val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

			val emailRegex = emailPattern.toRegex()
			if (!emailRegex.matches(it)) throw CheckError("String is not a valid email address")
		}
		return this
	}

	/**
	 * Checks if the string is a valid phone number.
	 * A valid phone number is defined as one that contains only digits, spaces, dashes, and parentheses.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid phone number.
	 */
	fun phone(): StringSchema {
		checks.add {
			val phonePattern = """^\+?[\d\s\-\(\)]{7,20}$"""
			val phoneRegex = phonePattern.toRegex()
			if (!phoneRegex.matches(it)) throw CheckError("String is not a valid phone number")
		}
		return this
	}

	/**
	 * Checks if the string matches the given regex pattern.
	 * @param pattern The regex pattern to match against.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string does not match the regex pattern.
	 */
	fun regex(pattern: Regex): StringSchema {
		checks.add { if (!pattern.matches(it)) throw CheckError("String does not match pattern") }
		return this
	}

	/*
	 * Checks if the string is a valid JSON string.
	 * A valid JSON string is defined as one that can be parsed into a Map<String, Any>.
	 * @return The current StringSchema instance for method chaining.
	 * @throws CheckError if the string is not a valid JSON string.
	 */
	fun optional(): StringSchema {
		isOptional = true
		return this
	}

	/**
	 * Checks if the string is nullable.
	 * A nullable string is defined as one that can be null.
	 * @return The current StringSchema instance for method chaining.
	 */
	fun nullable(): StringSchema {
		isNullable = true
		return this
	}

	/**
	 * Sets the default value for the string.
	 * @param value The default value to set.
	 * @return The current StringSchema instance for method chaining.
	 */
	fun default(value: String): StringSchema {
		defaultValue = value
		return this
	}

	/**
	 * Sets the default value for the string.
	 * @param value The default value to set.
	 * @return The current StringSchema instance for method chaining.
	 */
	override fun parse(value: Any?): String? {
	    if (value == null) {
	        if (isNullable) return null
	        if (isOptional) return defaultValue ?: ""
	        throw TypeError("Value is null")
	    }

	    if (value == Unit) { // Behandle den Fall für "undefined" (ähnlich wie in Zod)
	        if (isOptional) return defaultValue ?: ""
	        throw TypeError("Value is undefined")
	    }

	    val str = value as? String ?: throw TypeError("string: Expected a String, but got ${value::class.simpleName}")
	    checks.forEach { it(str) }

	    return str
	}

	//transformers

	fun trim(): StringSchema {
		checks.add { it.trim() }
		return this
	}

	fun lowercase(): StringSchema {
		checks.add { it.lowercase() }
		return this
	}

	fun uppercase(): StringSchema {
		checks.add { it.uppercase() }
		return this
	}

	fun firstCharToUpperCase(): StringSchema {
		checks.add { it.replaceFirstChar { char -> char.uppercase() } }
		return this
	}
}