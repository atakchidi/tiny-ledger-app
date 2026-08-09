package altak.ledger.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryTest {

    @Test
    fun `a cursor holds between one record and the most a page may carry`() {
        assertEquals(1, Cursor<String>(1).limit)
        assertEquals(Cursor.MAX_LIMIT, Cursor<String>(Cursor.MAX_LIMIT).limit)
    }

    @Test
    fun `orders by the id records were recorded under unless asked otherwise`() {
        assertEquals(Sorting.ID, Sorting().field)
        assertEquals(Sorting.Direction.ASC, Sorting().direction)
    }

    @Test
    fun `carries the cursor along when a page is mapped`() {
        val page = Page(listOf(1, 2), nextCursor = "next")

        assertEquals(listOf("1", "2"), page.map { it.toString() }.items)
        assertEquals("next", page.map { it.toString() }.nextCursor)
    }
}
