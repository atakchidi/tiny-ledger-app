package altak.ledger.domain

data class Cursor<ID>(val limit: Int, val after: ID? = null) {
    init {
        if (limit !in 1..MAX_LIMIT) throw InvalidLimit(limit)
    }

    class InvalidLimit(limit: Int) :
        LedgerException("A page holds between 1 and $MAX_LIMIT records, but asked for $limit")

    companion object {
        const val MAX_LIMIT = 200
    }
}

data class Page<T>(val items: List<T>, val nextCursor: String? = null) {

    fun <R> map(transform: (T) -> R): Page<R> = Page(items.map(transform), nextCursor)
}
