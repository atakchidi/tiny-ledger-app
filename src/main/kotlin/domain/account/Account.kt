package altak.ledger.domain.account

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.Money
import altak.ledger.domain.journal.Direction
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.MovementType
import java.util.Currency
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class Account(
    override val id: AccountId,
    val reference: AccountReference,
    val name: String,
    val currency: Currency,
    val type: AccountType,
    override val createdAt: Instant,
) : AggregateRoot<AccountId> {
    var updatedAt = createdAt
        private set

    var balance = Money.zero(currency)
        private set

    data class Movement(val lines: List<EntryLine>, val accounts: List<Account>)

    fun move(movement: MovementType, amount: Money, chart: ChartOfAccounts, clock: Clock): Movement {
        val counterpart = chart.of(movement.counterpart, currency)
        val line = EntryLine(id, type.direction(movement.effect), accept(amount))
        val counterLine = counterpart.counterLine(line)

        this.project(line, clock)
        counterpart.project(counterLine, clock)

        return Movement(
            lines = listOf(line, counterLine),
            accounts = listOf(this, counterpart),
        )
    }

    private fun counterLine(line: EntryLine) = EntryLine(id, line.direction.opposite, accept(line.amount))

    private fun project(line: EntryLine, clock: Clock) {
        require(line.accountId == id) { "Line for account ${line.accountId} cannot be projected onto $id" }

        balance += line.signedAgainst(type.direction)
        updatedAt = clock.now()
    }

    private fun accept(amount: Money): Money {
        require(amount.currency == currency) {
            "Account $name is held in ${currency.currencyCode}, " +
                "so it cannot take a ${amount.currency.currencyCode} amount"
        }

        return amount
    }
}

@JvmInline
value class AccountId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()
}

@JvmInline
value class AccountReference(val value: String) {
    init {
        require(value.matches(FORMAT.toRegex())) { "\"$value\" is not a reference: expected 3 to 32 letters, digits or dashes" }
    }

    override fun toString(): String = value

    companion object {
        const val FORMAT = "[A-Z0-9][A-Z0-9-]{2,31}"
    }
}

enum class AccountType(val direction: Direction) {
    ASSET(Direction.DEBIT), // what the business owns: cash, bank balances, receivables, inventory
    EXPENSE(Direction.DEBIT), // value consumed to run the business: salaries, rent, fees, interest paid
    LIABILITY(Direction.CREDIT), // what the business owes to others: loans, payables, customer deposits
    EQUITY(Direction.CREDIT), // owners' residual claim on the business: contributed capital, retained earnings
    REVENUE(Direction.CREDIT), // value earned from doing business: sales, interest and commission income
    ;

    fun direction(effect: Effect) = effect.sideOf(this)
}

enum class Effect {
    INCREASE {
        override fun sideOf(type: AccountType) = type.direction
    },
    DECREASE {
        override fun sideOf(type: AccountType) = type.direction.opposite
    },
    ;

    abstract fun sideOf(type: AccountType): Direction
}

enum class AccountRole(val type: AccountType, val title: String, private val prefix: String) {
    CASH(AccountType.ASSET, "Cash", "CASH"),
    ;

    fun referenceFor(currency: Currency) = AccountReference("$prefix-${currency.currencyCode}")
}

fun interface ChartOfAccounts {

    fun of(role: AccountRole, currency: Currency): Account
}
