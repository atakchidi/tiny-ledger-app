package altak.ledger.domain

import java.util.Currency
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JvmInline
value class AccountId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun of(value: String) = AccountId(Uuid.parse(value))
    }
}

enum class AccountType(val normalSide: Direction) {
    ASSET(Direction.DEBIT),
    LIABILITY(Direction.CREDIT),
}

data class Account(
    val id: AccountId,
    val name: String,
    val currency: Currency,
    val type: AccountType,
    val createdAt: Instant,
) {
    constructor(name: String, currency: Currency, type: AccountType, clock: Clock) :
        this(AccountId(Uuid.generateV7NonMonotonicAt(clock.now())), name, currency, type, clock.now())

    fun deposit(amount: Money, counterpart: Account, clock: Clock, description: String = "Deposit"): JournalEntry =
        JournalEntry(description, listOf(increase(amount), counterpart.increase(amount)), clock)

    fun withdraw(amount: Money, counterpart: Account, clock: Clock, description: String = "Withdrawal"): JournalEntry =
        JournalEntry(description, listOf(decrease(amount), counterpart.decrease(amount)), clock)

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
