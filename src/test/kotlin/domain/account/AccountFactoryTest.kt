package altak.ledger.domain.account

import altak.ledger.NOW
import altak.ledger.accountFactory
import altak.ledger.domain.Money
import altak.ledger.fixedClock
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountFactoryTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory(fixedClock())

    @Test
    fun `takes its creation time and a version 7 id from the clock`() {
        val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE"))

        assertEquals(NOW, alice.createdAt)
        assertEquals('7', alice.id.toString()[14])
    }

    @Test
    fun `opens a holder's account owing them nothing yet`() {
        val alice = factory.forHolder("Alice", eur, AccountReference("ACC-000123"))

        assertEquals("Alice", alice.name)
        assertEquals("ACC-000123", alice.reference.toString())
        assertEquals(AccountType.LIABILITY, alice.type)
        assertEquals(Money.zero(eur), alice.balance)
        assertEquals(alice.createdAt, alice.updatedAt)
    }

    @Test
    fun `takes an internal account's name, type and reference from the role it serves`() {
        val cash = factory.internal(AccountRole.CASH, eur)

        assertEquals("CASH-EUR", cash.reference.toString())
        assertEquals("Cash EUR", cash.name)
        assertEquals(AccountType.ASSET, cash.type)
    }

    @Test
    fun `names an internal account per currency, so each stands on its own`() {
        val usd = Currency.getInstance("USD")

        assertEquals("CASH-USD", factory.internal(AccountRole.CASH, usd).reference.toString())
    }
}
