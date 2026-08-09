package altak.ledger.domain

data class Cursor<ID>(
    val limit: Int,
    val after: ID? = null,
    val sorting: Sorting = Sorting(),
) {
    init {
        require(limit in 1..MAX_LIMIT) { "A page holds between 1 and $MAX_LIMIT records, but asked for $limit" }
    }

    companion object {
        const val MAX_LIMIT = 200
    }
}

data class Sorting(val field: String = ID, val direction: Direction = Direction.ASC) {

    enum class Direction { ASC, DESC }

    class UnknownField(field: String) :
        RuntimeException("\"$field\" is not a field these records can be ordered by")

    companion object {
        const val ID = "id"
    }
}

data class Page<T>(val items: List<T>, val nextCursor: String? = null) {

    fun <R> map(transform: (T) -> R) = Page(items.map(transform), nextCursor)
}
