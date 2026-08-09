package altak.ledger.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Currency
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface AggregateRoot<ID> {

    val id: ID

    val createdAt: Instant
}

data class Money(val minorUnits: BigInteger, val currency: Currency) {

    constructor(minorUnits: Long, currency: Currency) : this(BigInteger.valueOf(minorUnits), currency)

    constructor(amount: BigDecimal, currency: Currency) : this(amount.setScale(currency.fractionDigits).unscaledValue(), currency) {
        require(fits(amount, currency)) { "$amount is finer than ${currency.currencyCode} can hold" }
    }

    val isPositive: Boolean get() = minorUnits.signum() > 0

    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot combine ${currency.currencyCode} and ${other.currency.currencyCode} amounts"
        }

        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun unaryMinus() = copy(minorUnits = -minorUnits)

    fun toDecimal(): BigDecimal = BigDecimal(minorUnits, currency.fractionDigits)

    fun toPlainString(): String = toDecimal().toPlainString()

    companion object {
        fun zero(currency: Currency) = Money(BigInteger.ZERO, currency)

        // TODO: belongs in a class-level DTO constraint alongside the other validation, but the
        // request names an account rather than a currency, so the check needs the account fetched
        // first. Until then the application layer asks this before building an amount.
        fun fits(amount: BigDecimal, currency: Currency) =
            amount.stripTrailingZeros().scale() <= currency.fractionDigits
    }
}

private val Currency.fractionDigits: Int get() = defaultFractionDigits.coerceAtLeast(0)

fun interface IdGenerator {
    fun nextId(clock: Clock): Uuid
}

// A Clock hands out instants, which have no date until a zone says where midnight falls. The books
// keep one calendar for that: an accounting day is the same day for every caller, wherever they are.
class LedgerCalendar(private val clock: Clock, private val zone: TimeZone) {

    fun today() = clock.now().toLocalDateTime(zone).date
}
