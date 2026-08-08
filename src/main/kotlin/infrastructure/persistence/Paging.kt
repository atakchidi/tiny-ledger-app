package altak.ledger.infrastructure.persistence

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.Cursor
import altak.ledger.domain.Page
import altak.ledger.domain.Sorting

internal fun <ID, T : AggregateRoot<ID>> List<T>.pageFrom(cursor: Cursor<ID>): Page<T> {
    val ordered = sortedWith(cursor.sorting.comparator())

    val start = cursor.after
        ?.let { after -> ordered.indexOfFirst { it.id == after }.takeIf { it >= 0 }?.plus(1) ?: return Page(emptyList()) }
        ?: 0

    val window = ordered.drop(start).take(cursor.limit + 1)
    val items = window.take(cursor.limit)

    return Page(
        items = items,
        nextCursor = items.lastOrNull()?.id?.toString()?.takeIf { window.size > cursor.limit },
    )
}

// Id comes last in every order, since a page has to resume from the record after the one it was
// handed, and only the id tells two records apart.
private fun <T : AggregateRoot<*>> Sorting.comparator(): Comparator<T> {
    val byId = compareBy<T> { it.id.toString() }
    val ordering = if (field == Sorting.ID) byId else compareBy<T> { it.valueOf(field) }.then(byId)

    return if (direction == Sorting.Direction.DESC) ordering.reversed() else ordering
}

// A property whose type is a value class compiles to a mangled getter — getReference-a1b2c3 — so the
// lookup takes the prefix, and reads the unwrapped value, which is what ordering wants anyway.
private fun Any.valueOf(field: String): Comparable<*>? {
    val getterName = "get${field.replaceFirstChar(Char::uppercaseChar)}"
    val getter = javaClass.methods.firstOrNull {
        it.parameterCount == 0 && (it.name == getterName || it.name.startsWith("$getterName-"))
    } ?: throw Sorting.UnknownField(field)

    return getter.invoke(this)?.let { it as? Comparable<*> ?: throw Sorting.UnknownField(field) }
}
