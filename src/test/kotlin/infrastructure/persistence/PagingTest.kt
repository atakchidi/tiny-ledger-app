package altak.ledger.infrastructure.persistence

import altak.ledger.accountFactory
import altak.ledger.advancingClock
import altak.ledger.domain.Cursor
import altak.ledger.domain.Sorting
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PagingTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory(advancingClock())

    private val chloe = factory.forHolder("Chloe", eur, AccountReference("ACC-003"))
    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-001"))
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-002"))

    private val accounts = listOf(chloe, alice, bob)

    private fun page(sorting: Sorting = Sorting(), limit: Int = 50, after: Account? = null) =
        accounts.pageFrom(Cursor(limit, after?.id, sorting))

    @Test
    fun `orders by the id records were recorded under unless asked otherwise`() {
        assertEquals(listOf(chloe, alice, bob), page().items)
    }

    @Test
    fun `orders by any field the records carry`() {
        assertEquals(listOf(alice, bob, chloe), page(Sorting("name")).items)
        assertEquals(listOf(alice, bob, chloe), page(Sorting("reference")).items)
    }

    @Test
    fun `turns the order around`() {
        assertEquals(listOf(chloe, bob, alice), page(Sorting("name", Sorting.Direction.DESC)).items)
        assertEquals(listOf(bob, alice, chloe), page(Sorting(direction = Sorting.Direction.DESC)).items)
    }

    @Test
    fun `resumes a sorted page from the record it was handed`() {
        val firstPage = page(Sorting("name"), limit = 2)

        assertEquals(listOf(alice, bob), firstPage.items)
        assertEquals(bob.id.toString(), firstPage.nextCursor)

        val lastPage = page(Sorting("name"), limit = 2, after = bob)

        assertEquals(listOf(chloe), lastPage.items)
        assertNull(lastPage.nextCursor)
    }

    @Test
    fun `refuses a field the records do not carry`() {
        assertFailsWith<Sorting.UnknownField> { page(Sorting("whatever")) }
    }

    @Test
    fun `refuses a field nothing could order by`() {
        assertFailsWith<Sorting.UnknownField> { page(Sorting("balance")) }
    }
}
