# Präsentation des DaBi-Framework Validierungs-Pakets

Dieses Notebook demonstriert die Verwendung des `validation`-Pakets im DaBi-Framework, das eine Domain Specific Language (DSL) zur Definition und Validierung von Daten-Schemas bietet.

## 1. Importe

Zuerst importieren wir die notwendigen Klassen aus dem `validation`-Paket.

```kotlin
import validation.schema.dsl.*
import validation.error.ValidationError
import org.json.JSONObject
```

## 2. Schema-Definition

Das `validation`-Paket ermöglicht die Definition von Schemas mit einer intuitiven DSL. Hier sind einige Beispiele für verschiedene Datentypen.

### 2.1 String-Schema

Ein einfaches String-Schema mit Mindest- und Höchstlänge.

```kotlin
val stringSchema = schema {
    string("name") {
        minLength(3)
        maxLength(50)
    }
}

println("String Schema: $stringSchema")
```

### 2.2 Number-Schema

Ein Zahlen-Schema mit Mindest- und Höchstwert.

```kotlin
val numberSchema = schema {
    number("age") {
        min(0)
        max(120)
    }
}

println("Number Schema: $numberSchema")
```

### 2.3 Boolean-Schema

Ein einfaches Boolean-Schema.

```kotlin
val booleanSchema = schema {
    boolean("isActive")
}

println("Boolean Schema: $booleanSchema")
```

### 2.4 Object-Schema

Ein Objekt-Schema, das verschachtelte Schemas und erforderliche Felder definiert.

```kotlin
val userSchema = schema {
    obj("user") {
        required("name", "age", "email")
        string("name") { minLength(3) }
        number("age") { min(18) }
        string("email") { pattern("^[\\w\\.-]+@[\\w\\.-]+\\.\\w+$") }
        boolean("isAdmin") { optional() }
    }
}

println("User Schema: $userSchema")
```

### 2.5 Array-Schema

Ein Array-Schema, das den Typ der Elemente und die Mindest-/Höchstanzahl der Elemente definiert.

```kotlin
val tagsSchema = schema {
    array("tags") {
        stringElements { minLength(2) }
        minItems(1)
        maxItems(5)
    }
}

println("Tags Schema: $tagsSchema")
```

## 3. Daten-Validierung

Nachdem wir Schemas definiert haben, können wir Daten gegen diese Schemas validieren.

### 3.1 Erfolgreiche Validierung

```kotlin
val validUserData = JSONObject("""
{
    "user": {
        "name": "Alice",
        "age": 30,
        "email": "alice@example.com",
        "isAdmin": false
    }
}
""")

val validationResult = userSchema.validate(validUserData)

if (validationResult.isValid) {
    println("Validierung erfolgreich: ${validationResult.isValid}")
} else {
    println("Validierung fehlgeschlagen: ${validationResult.errors}")
}
```

### 3.2 Fehlgeschlagene Validierung

```kotlin
val invalidUserData = JSONObject("""
{
    "user": {
        "name": "Al",
        "age": 17,
        "email": "invalid-email",
        "isAdmin": "yes"
    }
}
""")

val invalidValidationResult = userSchema.validate(invalidUserData)

if (invalidValidationResult.isValid) {
    println("Validierung erfolgreich: ${invalidValidationResult.isValid}")
} else {
    println("Validierung fehlgeschlagen. Fehler:")
    invalidValidationResult.errors.forEach { error ->
        println("- ${error.message} (Pfad: ${error.path})")
    }
}
```

### 3.3 Validierung eines Arrays

```kotlin
val validTagsData = JSONObject("""
{
    "tags": ["kotlin", "gradle"]
}
""")

val tagsValidationResult = tagsSchema.validate(validTagsData)

if (tagsValidationResult.isValid) {
    println("Tags Validierung erfolgreich: ${tagsValidationResult.isValid}")
} else {
    println("Tags Validierung fehlgeschlagen: ${tagsValidationResult.errors}")
}

val invalidTagsData = JSONObject("""
{
    "tags": ["a", "b", "c", "d", "e", "f"]
}
""")

val invalidTagsValidationResult = tagsSchema.validate(invalidTagsData)

if (invalidTagsValidationResult.isValid) {
    println("Invalid Tags Validierung erfolgreich: ${invalidTagsValidationResult.isValid}")
} else {
    println("Invalid Tags Validierung fehlgeschlagen. Fehler:")
    invalidTagsValidationResult.errors.forEach { error ->
        println("- ${error.message} (Pfad: ${error.path})")
    }
}
```

## 4. Fehlerbehandlung

Fehler bei der Validierung werden als Liste von `ValidationError`-Objekten zurückgegeben, die Details wie die Fehlermeldung und den Pfad zum fehlerhaften Feld enthalten.

Dies war eine kurze Einführung in das `validation`-Paket des DaBi-Frameworks. Es bietet eine flexible und typsichere Möglichkeit, Daten-Schemas in Kotlin zu definieren und zu validieren.
