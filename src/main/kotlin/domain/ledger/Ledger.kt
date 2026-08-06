package altak.ledger.domain.ledger

import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.account.AccountType
import java.util.Currency
import kotlin.time.Clock

class Ledger(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transactions: TransactionManager,
    private val clock: Clock,
) {
    fun open(name: String, currency: Currency): Account =
        transactions {
            Account(name, currency, AccountType.LIABILITY, clock).also { accounts.save(it) }
        }

    fun accountOf(id: AccountId): Account =
        accounts.byId(id) ?: throw LedgerException.AccountNotFound("No account with id $id")

    fun balanceOf(id: AccountId): Money = accountOf(id).balance

    fun historyOf(id: AccountId, page: Page = Page()): List<JournalEntry> =
        entries.byAccount(accountOf(id).id, page)

    fun deposit(into: AccountId, amount: Money, description: String = "Deposit"): JournalEntry =
        transactions { post(accountOf(into).deposit(amount, cashIn(amount.currency), clock, description)) }

    fun withdraw(from: AccountId, amount: Money, description: String = "Withdrawal"): JournalEntry =
        transactions { post(accountOf(from).withdraw(amount, cashIn(amount.currency), clock, description)) }

    private fun post(entry: JournalEntry): JournalEntry {
        entry.lines.forEach { line -> accounts.save(accountOf(line.accountId).record(line)) }
        entries.save(entry)
        return entry
    }

    private fun cashIn(currency: Currency): Account =
        accounts.cashIn(currency)
            ?: Account("Cash ${currency.currencyCode}", currency, AccountType.ASSET, clock).also { accounts.save(it) }
}
