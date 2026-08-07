package altak.ledger.infrastructure.persistence

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.Cursor
import altak.ledger.domain.Page

internal fun <ID, T : AggregateRoot<ID>> List<T>.pageFrom(cursor: Cursor<ID>): Page<T> {
    val start = cursor.after
        ?.let { after -> indexOfFirst { it.id == after }.takeIf { it >= 0 }?.plus(1) ?: return Page(emptyList()) }
        ?: 0

    val window = drop(start).take(cursor.limit + 1)
    val items = window.take(cursor.limit)

    return Page(
        items = items,
        nextCursor = items.lastOrNull()?.id?.toString()?.takeIf { window.size > cursor.limit },
    )
}
