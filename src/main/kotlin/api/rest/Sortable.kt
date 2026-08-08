package altak.ledger.api.rest

import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.Sorting
import io.ktor.server.plugins.requestvalidation.RequestValidationException

/**
 * The fields an endpoint will order its records by. A repository can order by any field a record has,
 * which is more than a caller should reach into, so each route publishes its own few — documented and
 * enforced from the same value. The id is always among them.
 */
class Sortable(vararg fields: String) {

    val fields = listOf(Sorting.ID) + fields.filterNot { it == Sorting.ID }
}

fun CursorDto.sortedWithin(sortable: Sortable) = also {
    if (sort != null && sort !in sortable.fields) {
        throw RequestValidationException(this, listOf("sort: must be one of ${sortable.fields.joinToString()}"))
    }
}
