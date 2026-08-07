package altak.ledger.domain.account

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.ISSUED_VERSION
import altak.ledger.domain.version
import altak.ledger.domain.entry.Direction
import altak.ledger.domain.entry.EntryLine
import java.util.Currency
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JvmInline
value class AccountId(val value: Uuid) {
    init {
        if (value.version != ISSUED_VERSION) throw Malformed(value.toString())
    }

    constructor(value: String) : this(Uuid.parse(value))

    class Malformed(id: String) : LedgerException("\"$id\" is not an identifier this ledger issues")

    override fun toString(): String = value.toString()
}

@JvmInline
value class AccountReference(val value: String) {
    init {
        if (!value.matches(FORMAT.toRegex())) throw Malformed(value)
    }

    class Malformed(reference: String) :
        LedgerException("\"$reference\" is not a reference: expected 3 to 32 letters, digits or dashes")

    override fun toString(): String = value

    companion object {
        const val FORMAT = "[A-Z0-9][A-Z0-9-]{2,31}"

        fun normalized(raw: String) = AccountReference(raw.trim().uppercase())
    }
}

enum class AccountType(val normalSide: Direction) {
    ASSET(Direction.DEBIT),
    EXPENSE(Direction.DEBIT),
    LIABILITY(Direction.CREDIT),
    EQUITY(Direction.CREDIT),
    REVENUE(Direction.CREDIT),
}

enum class Effect { INCREASE, DECREASE }

enum class AccountRole(val type: AccountType, val title: String, private val prefix: String) {
    CASH(AccountType.ASSET, "Cash", "CASH"),
    ;

    fun referenceFor(currency: Currency) = AccountReference.normalized("$prefix-${currency.currencyCode}")
}

fun interface ChartOfAccounts {

    fun of(role: AccountRole, currency: Currency): Account
}

data class Account(
    override val id: AccountId,
    val reference: AccountReference,
    val name: String,
    val currency: Currency,
    val type: AccountType,
    override val createdAt: Instant,
    override val updatedAt: Instant = createdAt,
    val balance: Money = Money.zero(currency),
) : AggregateRoot<AccountId> {

    class CurrencyMismatch(message: String) : LedgerException(message)

    class ForeignLine(message: String) : LedgerException(message)

    companion object {
        fun forHolder(
            name: String,
            currency: Currency,
            clock: Clock,
            reference: AccountReference? = null,
        ): Account {
            val id = AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))

            return Account(
                id = id,
                reference = reference ?: AccountReference.normalized("ACC-${id.value.toString().takeLast(12)}"),
                name = name,
                currency = currency,
                type = AccountType.LIABILITY,
                createdAt = clock.now(),
            )
        }

        fun internal(role: AccountRole, currency: Currency, clock: Clock): Account {
            val id = AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))

            return Account(
                id = id,
                reference = role.referenceFor(currency),
                name = "${role.title} ${currency.currencyCode}",
                currency = currency,
                type = role.type,
                createdAt = clock.now(),
            )
        }
    }

    fun sideFor(effect: Effect): Direction =
        if (effect == Effect.INCREASE) type.normalSide else type.normalSide.opposite

    fun line(side: Direction, amount: Money): EntryLine = EntryLine(id, side, accept(amount))

    fun project(line: EntryLine, clock: Clock): Account {
        if (line.accountId != id) {
            throw ForeignLine("Line for account ${line.accountId} cannot be projected onto $id")
        }
        return copy(
            balance = balance + line.signedAgainst(type.normalSide),
            updatedAt = clock.now(),
        )
    }

    private fun accept(amount: Money): Money {
        if (amount.currency != currency) {
            throw CurrencyMismatch(
                "Account $name is held in ${currency.currencyCode}, " +
                    "so it cannot take a ${amount.currency.currencyCode} amount",
            )
        }
        return amount
    }
}
