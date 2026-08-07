package altak.ledger.domain.ledger

import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import kotlin.time.Clock

data class Posting(val entry: JournalEntry, val accounts: List<Account>)

class Ledger(
    private val holder: Account,
    private val cash: Account,
    private val clock: Clock,
) {
    class CurrencyMismatch(message: String) : LedgerException(message)

    init {
        if (holder.currency != cash.currency) {
            throw CurrencyMismatch(
                "${holder.name} is held in ${holder.currency.currencyCode}, " +
                    "which ${cash.name} cannot settle",
            )
        }
    }

    fun deposit(amount: Money, description: String? = null): Posting =
        posting(holder.deposit(amount, cash, clock, description ?: DEPOSIT))

    fun withdraw(amount: Money, description: String? = null): Posting =
        posting(holder.withdraw(amount, cash, clock, description ?: WITHDRAWAL))

    private fun posting(entry: JournalEntry) = Posting(
        entry = entry,
        accounts = listOf(holder, cash).map { account ->
            entry.lines.filter { it.accountId == account.id }
                .fold(account) { updated, line -> updated.record(line, clock) }
        },
    )

    private companion object {
        const val DEPOSIT = "Deposit"
        const val WITHDRAWAL = "Withdrawal"
    }
}
