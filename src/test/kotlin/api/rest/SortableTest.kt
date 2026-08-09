package altak.ledger.api.rest

import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.Sorting
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SortableTest {

    private val sortable = Sortable("occurredOn")

    @Test
    fun `offers the id alongside the fields the route publishes`() {
        assertEquals(listOf(Sorting.ID, "occurredOn"), sortable.fields)
    }

    @Test
    fun `offers the id once, however the route names it`() {
        assertEquals(listOf(Sorting.ID, "reference"), Sortable("id", "reference").fields)
    }

    @Test
    fun `passes a cursor that names a field the route offers, or none at all`() {
        assertEquals("occurredOn", CursorDto(sort = "occurredOn").sortedWithin(sortable).sort)
        assertEquals(null, CursorDto().sortedWithin(sortable).sort)
    }

    @Test
    fun `refuses a field the route does not offer, naming the ones it does`() {
        val refused = assertFailsWith<RequestValidationException> {
            CursorDto(sort = "description").sortedWithin(sortable)
        }

        assertEquals(listOf("sort: must be one of id, occurredOn"), refused.reasons)
    }
}
