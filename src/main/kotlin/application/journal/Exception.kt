package altak.ledger.application.journal

import altak.ledger.application.shared.StatusCode
import altak.ledger.application.shared.UseCaseException
import java.math.BigDecimal
import java.util.Currency

@StatusCode(400)
class AmountTooPrecise(amount: BigDecimal, currency: Currency) :
    UseCaseException("$amount is finer than ${currency.currencyCode} can hold")
