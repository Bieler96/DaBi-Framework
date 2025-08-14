package di.annotations

/**
 * Annotation zur Kennzeichnung eines Konstruktors, einer Methode oder einer Eigenschaft,
 * die Abhängigkeiten injiziert bekommen soll.
 */
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject