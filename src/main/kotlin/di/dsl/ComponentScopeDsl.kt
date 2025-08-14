package di.dsl

/**
 * DSL für die Definition von Komponentenbereichen
 */
class ComponentScope(private val name: String) {
    private val components = mutableListOf<Component>()

    fun component(name: String, init: Component.() -> Unit) {
        val component = Component(name)
        component.init()
        components.add(component)
    }

    fun getComponents(): List<Component> = components
}

/**
 * Komponente innerhalb eines Bereichs
 */
class Component(val name: String) {
    private val dependencies = mutableListOf<String>()
    private var isSingleton: Boolean = true

    fun dependsOn(vararg names: String) {
        dependencies.addAll(names)
    }

    fun singleton() {
        isSingleton = true
    }

    fun factory() {
        isSingleton = false
    }

    fun getDependencies(): List<String> = dependencies
    fun isSingleton(): Boolean = isSingleton
}

/**
 * Einstiegspunkt für die Komponenten-DSL
 */
fun defineComponents(init: ComponentScope.() -> Unit): ComponentScope {
    val scope = ComponentScope("root")
    scope.init()
    return scope
}