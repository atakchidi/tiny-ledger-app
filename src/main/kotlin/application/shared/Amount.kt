package altak.ledger.application.shared

import altak.ledger.domain.Money
import java.math.BigDecimal
import java.util.Currency

fun Money.toDecimal(): String = BigDecimal.valueOf(minorUnits, currency.fractionDigits()).toPlainString()

fun String.toMoney(currency: Currency): Money {
    val decimal = try {
        BigDecimal(this)
    } catch (notANumber: NumberFormatException) {
        throw MalformedAmount("\"$this\" is not a decimal amount")
    }

    val minorUnits = try {
        decimal.setScale(currency.fractionDigits()).unscaledValue().longValueExact()
    } catch (tooPrecise: ArithmeticException) {
        throw MalformedAmount(
            "\"$this\" cannot be held in ${currency.currencyCode}, " +
                "which has ${currency.fractionDigits()} decimal places",
        )
    }

    return Money(minorUnits, currency)
}

private fun Currency.fractionDigits(): Int = defaultFractionDigits.coerceAtLeast(0)
