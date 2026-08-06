package altak.ledger.domain

import java.util.Currency

sealed class LedgerException(message: String) : RuntimeException(message) {

    class UnknownCurrency(message: String) : LedgerException(message)

    class UnbalancedEntry(message: String) : LedgerException(message)

    class CurrencyMismatch(message: String) : LedgerException(message)

    class MalformedEntry(message: String) : LedgerException(message)

    class AccountNotFound(message: String) : LedgerException(message)
}

fun currencyOf(code: String): Currency =
    try {
        Currency.getInstance(code)
    } catch (unknown: IllegalArgumentException) {
        throw LedgerException.UnknownCurrency("\"$code\" is not an ISO 4217 currency code")
    }

data class Money(val minorUnits: Long, val currency: Currency) {

    val isPositive: Boolean get() = minorUnits > 0

    operator fun plus(other: Money): Money {
        if (currency != other.currency) {
            throw LedgerException.CurrencyMismatch(
                "Cannot combine ${currency.currencyCode} and ${other.currency.currencyCode} amounts",
            )
        }
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    companion object {
        fun zero(currency: Currency) = Money(0, currency)
    }
}
