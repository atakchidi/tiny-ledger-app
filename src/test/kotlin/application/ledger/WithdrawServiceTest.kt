package altak.ledger.application.ledger

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.ledger.service.Withdraw
import altak.ledger.application.ledger.service.WithdrawService
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.currencyOf
import altak.ledger.domain.ledger.Page
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WithdrawServiceTest {

    private val eur = currencyOf("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val service = WithdrawService(accounts, entries, transactions, clock)

    private val alice = Account.forHolder("Alice", eur, clock)
        .copy(balance = Money(1050, eur))
        .also(accounts::save)

    private val cash = Account.forCash(eur, clock)
        .copy(balance = Money(1050, eur))
        .also(accounts::save)

    private fun withdraw(amount: String, description: String? = null) =
        service.execute(Withdraw(alice.id.toString(), MovementDto(amount, description)))

    @Test
    fun `records the entry the other way round from a deposit`() {
        val entry = withdraw("4.00")

        assertEquals("Withdrawal", entry.description)
        assertEquals("DEBIT", entry.lines.single { it.accountId == alice.id.toString() }.direction)
        assertEquals("CREDIT", entry.lines.single { it.accountId == cash.id.toString() }.direction)
    }

    @Test
    fun `lowers what the ledger owes and what it holds alike`() {
        withdraw("4.00")

        assertEquals(Money(650, eur), accounts.byId(alice.id)?.balance)
        assertEquals(Money(650, eur), accounts.byId(cash.id)?.balance)
    }

    @Test
    fun `lets the balance fall below zero`() {
        withdraw("15.00")

        assertEquals(Money(-450, eur), accounts.byId(alice.id)?.balance)
    }

    @Test
    fun `keeps the entry where both accounts can find it`() {
        val entry = withdraw("4.00")

        assertEquals(listOf(entry.id), entries.byAccount(alice.id, Page()).map { it.id.toString() })
        assertEquals(listOf(entry.id), entries.byAccount(cash.id, Page()).map { it.id.toString() })
    }

    @Test
    fun `carries the caller's own description`() {
        assertEquals("Rent", withdraw("4.00", "Rent").description)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> { service.execute(Withdraw("not-an-account", MovementDto("4.00"))) }
    }

    @Test
    fun `runs in one transaction`() {
        withdraw("4.00")

        assertEquals(1, transactions.transactions)
    }
}
