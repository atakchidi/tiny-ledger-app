package altak.ledger.application.account

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId

fun Account.toViewDto() = ViewAccountDto(
    id = id.value,
    reference = reference.toString(),
    name = name,
    currency = currency,
    type = type,
    balance = balance.toPlainString(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
