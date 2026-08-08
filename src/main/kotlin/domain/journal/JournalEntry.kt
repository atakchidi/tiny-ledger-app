package altak.ledger.domain.journal

import altak.ledger.domain.AggregateRoot
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountId
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class JournalEntry(
    override val id: EntryId,
    val description: String,
    val occurredOn: LocalDate,
    override val createdAt: Instant,
    val lines: List<EntryLine>,
    override val updatedAt: Instant = createdAt,
) : AggregateRoot<EntryId> {

    val currency get() = lines.first().amount.currency

    val debited: Money by lazy { totalFor(Direction.DEBIT) }

    val credited: Money by lazy { totalFor(Direction.CREDIT) }

    init {
        require(lines.size >= 2) { "An entry needs at least two lines, but had ${lines.size}" }

        val currencies = lines.map { it.amount.currency }.distinct()
        require(currencies.size == 1) {
            "All lines of an entry must share one currency, but found ${currencies.joinToString { it.currencyCode }}"
        }

        require(debited == credited) {
            "Debits must equal credits, but debited ${debited.minorUnits} and credited ${credited.minorUnits}"
        }
    }

    private fun totalFor(direction: Direction) =
        lines
            .filter { it.direction == direction }
            .fold(Money.zero(lines.first().amount.currency)) { running, line -> running + line.amount }
}

enum class Direction {
    DEBIT,
    CREDIT;

    val opposite
        get() = when (this) {
            DEBIT -> CREDIT
            CREDIT -> DEBIT
        }

    fun signOn(direction: Direction, amount: Money) = if (this == direction) amount else -amount
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
        require(amount.isPositive) {
            "A line amount must be positive, but was ${amount.minorUnits}"
        }
    }

    fun signedAgainst(normalSide: Direction) = direction.signOn(normalSide, amount)
}
