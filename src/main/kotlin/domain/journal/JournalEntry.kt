package altak.ledger.domain.journal

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.IdGenerator
import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class Direction {
    DEBIT {
        override val opposite get() = CREDIT
    },
    CREDIT {
        override val opposite get() = DEBIT
    },
    ;

    abstract val opposite: Direction

    fun signOn(normalSide: Direction, amount: Money): Money = if (this == normalSide) amount else -amount
}

@JvmInline
value class EntryId(val value: Uuid) {
    override fun toString(): String = value.toString()
    constructor(generator: IdGenerator, clock: Clock) : this(generator.nextId(clock))
}

data class EntryLine(
    val accountId: AccountId,
    val direction: Direction,
    val amount: Money,
) {
    class NonPositiveAmount(message: String) : LedgerException(message)

    init {
        if (!amount.isPositive) {
            throw NonPositiveAmount("A line amount must be positive, but was ${amount.minorUnits}")
        }
    }

    fun signedAgainst(normalSide: Direction): Money = direction.signOn(normalSide, amount)
}

data class JournalEntry(
    override val id: EntryId,
    val description: String,
    override val createdAt: Instant,
    val lines: List<EntryLine>,
    override val updatedAt: Instant = createdAt,
) : AggregateRoot<EntryId> {

    constructor(description: String, lines: List<EntryLine>, ids: IdGenerator, clock: Clock) :
        this(EntryId(ids, clock), description, clock.now(), lines)

    class TooFewLines(message: String) : LedgerException(message)

    class MixedCurrencies(message: String) : LedgerException(message)

    class Unbalanced(message: String) : LedgerException(message)

    val debited: Money by lazy { totalFor(Direction.DEBIT) }

    val credited: Money by lazy { totalFor(Direction.CREDIT) }

    init {
        if (lines.size < 2) {
            throw TooFewLines("An entry needs at least two lines, but had ${lines.size}")
        }

        val currencies = lines.map { it.amount.currency }.distinct()
        if (currencies.size > 1) {
            throw MixedCurrencies(
                "All lines of an entry must share one currency, but found " +
                    currencies.joinToString { it.currencyCode },
            )
        }

        if (debited != credited) {
            throw Unbalanced(
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
