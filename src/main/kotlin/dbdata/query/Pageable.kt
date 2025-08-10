package dbdata.query

interface Pageable {
    val pageNumber: Int
    val pageSize: Int
    val sort: Sort?

    val offset: Long
        get() = pageNumber.toLong() * pageSize.toLong()
}

data class PageRequest(
    override val pageNumber: Int,
    override val pageSize: Int,
    override val sort: Sort? = null
) : Pageable {
    init {
        require(pageNumber >= 0) { "Page number must not be less than zero!" }
        require(pageSize > 0) { "Page size must not be less than one!" }
    }
}
