package altak.ledger.domain

data class Cursor<ID>(
    val limit: Int,
    val after: ID? = null,
    val sorting: Sorting = Sorting(),
) {
    init {
        if (limit !in 1..MAX_LIMIT) throw InvalidLimit(limit)
    }

    class InvalidLimit(limit: Int) :
        LedgerException("A page holds between 1 and $MAX_LIMIT records, but asked for $limit")

    companion object {
        const val MAX_LIMIT = 200
    }
}

// The field is whatever the records are ordered by; the repository knows which of its fields those
// are. Id is the fallback because it is the one field every record has and every record has uniquely,
// which is what a cursor needs to resume from.
data class Sorting(val field: String = ID, val direction: Direction = Direction.ASC) {

    enum class Direction { ASC, DESC }

    class UnknownField(field: String) :
        LedgerException("\"$field\" is not a field these records can be ordered by")

    companion object {
        const val ID = "id"
    }
}

data class Page<T>(val items: List<T>, val nextCursor: String? = null) {

    fun <R> map(transform: (T) -> R): Page<R> = Page(items.map(transform), nextCursor)
}
