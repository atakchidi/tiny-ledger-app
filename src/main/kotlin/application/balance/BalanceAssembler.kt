package altak.ledger.application.balance

import altak.ledger.domain.entry.Balance

fun Balance.toViewDto() = ViewBalanceDto(
    onDate = onDate,
    accountId = account.id.toString(),
    reference = account.reference.toString(),
    currency = account.currency,
    amount = amount.toDecimal(),
)
