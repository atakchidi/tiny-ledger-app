package altak.ledger.application.journal

import altak.ledger.application.shared.StatusCode
import java.math.BigDecimal
import java.util.Currency

@StatusCode(400)
class AmountTooPrecise(amount: BigDecimal, currency: Currency) :
    RuntimeException("$amount is finer than ${currency.currencyCode} can hold")
