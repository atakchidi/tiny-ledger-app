package altak.ledger.domain.account

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.ledger.Direction
import altak.ledger.domain.ledger.EntryLine
import altak.ledger.domain.ledger.JournalEntry
import java.util.Currency
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JvmInline
value class AccountId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()
}

enum class AccountType(val normalSide: Direction) {
    ASSET(Direction.DEBIT),
    LIABILITY(Direction.CREDIT),
}

data class Account(
    override val id: AccountId,
    val name: String,
    val currency: Currency,
    val type: AccountType,
    override val createdAt: Instant,
    override val updatedAt: Instant = createdAt,
    val balance: Money = Money.zero(currency),
) : AggregateRoot<AccountId> {

    constructor(name: String, currency: Currency, type: AccountType, clock: Clock) :
        this(AccountId(Uuid.generateV7NonMonotonicAt(clock.now())), name, currency, type, clock.now())

    companion object {
        fun forHolder(name: String, currency: Currency, clock: Clock) =
            Account(name, currency, AccountType.LIABILITY, clock)

        fun forCash(currency: Currency, clock: Clock) =
            Account("Cash ${currency.currencyCode}", currency, AccountType.ASSET, clock)
    }

    fun deposit(amount: Money, counterpart: Account, clock: Clock, description: String): JournalEntry =
        JournalEntry(description, listOf(increase(amount), counterpart.increase(amount)), clock)

    fun withdraw(amount: Money, counterpart: Account, clock: Clock, description: String): JournalEntry =
        JournalEntry(description, listOf(decrease(amount), counterpart.decrease(amount)), clock)

    fun record(line: EntryLine, clock: Clock): Account {
        if (line.accountId != id) {
            throw LedgerException.MalformedEntry("Line for account ${line.accountId} cannot be recorded on $id")
        }
        return copy(
            balance = balance + line.signedAgainst(type.normalSide),
            updatedAt = clock.now(),
        )
    }

    private fun increase(amount: Money): EntryLine = EntryLine(id, type.normalSide, accept(amount))

    private fun decrease(amount: Money): EntryLine = EntryLine(id, type.normalSide.opposite, accept(amount))

    private fun accept(amount: Money): Money {
        if (amount.currency != currency) {
            throw LedgerException.CurrencyMismatch(
                "Account $name is held in ${currency.currencyCode}, " +
                    "so it cannot take a ${amount.currency.currencyCode} amount",
            )
        }
        return amount
    }
}
