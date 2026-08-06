package altak.ledger.domain

import java.util.Currency
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class Ledger(
    private val clock: Clock,
    accounts: List<Account> = emptyList(),
    entries: List<JournalEntry> = emptyList(),
) {
    private val accountsById = accounts.associateByTo(LinkedHashMap()) { it.id }
    private val recordedEntries = entries.toMutableList()

    val accounts: List<Account> get() = accountsById.values.toList()

    // An account holder's money is money this ledger owes them, so their account is a liability.
    // Its counterpart is the cash the ledger holds in that currency, which is an asset.
    fun open(name: String, currency: Currency): Account =
        register(Account(name, currency, AccountType.LIABILITY, clock))

    fun accountOf(id: AccountId): Account =
        accountsById[id] ?: throw LedgerException.AccountNotFound("No account with id $id")

    fun balanceOf(id: AccountId): Money {
        val account = accountOf(id)
        return recordedEntries
            .flatMap { it.lines }
            .filter { it.accountId == account.id }
            .fold(Money.zero(account.currency)) { running, line ->
                running + line.signedAgainst(account.type.normalSide)
            }
    }

    fun historyOf(id: AccountId): List<JournalEntry> {
        val account = accountOf(id)
        return recordedEntries.filter { it.touches(account.id) }
    }

    fun deposit(into: AccountId, amount: Money, description: String = "Deposit"): JournalEntry =
        record(accountOf(into).deposit(amount, cashIn(amount.currency), clock, description))

    fun withdraw(from: AccountId, amount: Money, description: String = "Withdrawal"): JournalEntry =
        record(accountOf(from).withdraw(amount, cashIn(amount.currency), clock, description))

    private fun record(entry: JournalEntry): JournalEntry = entry.also { recordedEntries += it }

    private fun register(account: Account): Account = account.also { accountsById[it.id] = it }

    private fun cashIn(currency: Currency): Account =
        accountsById.values.find { it.type == AccountType.ASSET && it.currency == currency }
            ?: register(Account("Cash ${currency.currencyCode}", currency, AccountType.ASSET, clock))
}

enum class Direction {
    DEBIT,
    CREDIT,
    ;

    val opposite: Direction get() = if (this == DEBIT) CREDIT else DEBIT
}

@JvmInline
value class EntryId(val value: Uuid) {
    override fun toString(): String = value.toString()
}

data class EntryLine(
    val accountId: AccountId,
    val direction: Direction,
    val amount: Money,
) {
    init {
        if (!amount.isPositive) {
            throw LedgerException.MalformedEntry("A line amount must be positive, but was ${amount.minorUnits}")
        }
    }

    fun signedAgainst(normalSide: Direction): Money = if (direction == normalSide) amount else -amount
}

data class JournalEntry(
    val id: EntryId,
    val description: String,
    val occurredAt: Instant,
    val lines: List<EntryLine>,
) {
    constructor(description: String, lines: List<EntryLine>, clock: Clock) :
        this(EntryId(Uuid.generateV7NonMonotonicAt(clock.now())), description, clock.now(), lines)

    init {
        if (lines.size < 2) {
            throw LedgerException.MalformedEntry("An entry needs at least two lines, but had ${lines.size}")
        }

        val currencies = lines.map { it.amount.currency }.distinct()
        if (currencies.size > 1) {
            throw LedgerException.CurrencyMismatch(
                "All lines of an entry must share one currency, but found " +
                    currencies.joinToString { it.currencyCode },
            )
        }

        val debited = totalFor(Direction.DEBIT)
        val credited = totalFor(Direction.CREDIT)
        if (debited != credited) {
            throw LedgerException.UnbalancedEntry(
                "Debits must equal credits, but debited ${debited.minorUnits} and credited ${credited.minorUnits}",
            )
        }
    }

    fun touches(accountId: AccountId): Boolean = lines.any { it.accountId == accountId }

    private fun totalFor(direction: Direction): Money =
        lines
            .filter { it.direction == direction }
            .fold(Money.zero(lines.first().amount.currency)) { running, line -> running + line.amount }
}
