package altak.ledger.domain

import java.math.BigDecimal
import java.util.Currency
import kotlin.time.Instant
import kotlin.uuid.Uuid

abstract class LedgerException(message: String) : RuntimeException(message)

const val ISSUED_VERSION = 7

val Uuid.version: Int get() = toLongs { mostSignificantBits, _ -> ((mostSignificantBits shr 12) and 0xF).toInt() }

interface AggregateRoot<ID> {

    val id: ID

    val createdAt: Instant

    val updatedAt: Instant
}

data class Money(val minorUnits: Long, val currency: Currency) {

    class CurrencyMismatch(message: String) : LedgerException(message)

    class MalformedAmount(message: String) : LedgerException(message)

    val isPositive: Boolean get() = minorUnits > 0

    operator fun plus(other: Money): Money {
        if (currency != other.currency) {
            throw CurrencyMismatch(
                "Cannot combine ${currency.currencyCode} and ${other.currency.currencyCode} amounts",
            )
        }
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    fun toDecimal(): BigDecimal = BigDecimal.valueOf(minorUnits, currency.fractionDigits)

    companion object {
        fun zero(currency: Currency) = Money(0, currency)

        fun of(amount: BigDecimal, currency: Currency): Money =
            try {
                Money(amount.setScale(currency.fractionDigits).unscaledValue().longValueExact(), currency)
            } catch (tooPrecise: ArithmeticException) {
                throw MalformedAmount(
                    "$amount cannot be held in ${currency.currencyCode}, " +
                        "which has ${currency.fractionDigits} decimal places",
                )
            }

        private val Currency.fractionDigits: Int get() = defaultFractionDigits.coerceAtLeast(0)
    }
}
