package dbdata.query

data class Sort(val orders: List<Order>) {
    constructor(property: String, direction: Direction = Direction.ASC) : this(listOf(Order(property, direction)))

    enum class Direction {
        ASC, DESC
    }

    data class Order(val property: String, val direction: Direction)
}
