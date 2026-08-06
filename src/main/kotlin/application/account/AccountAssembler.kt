package altak.ledger.application.account

import altak.ledger.application.shared.toDecimal
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId

fun String.toAccountId(): AccountId =
    try {
        AccountId(this)
    } catch (notAnId: IllegalArgumentException) {
        throw AccountNotFound(this)
    }

fun Account.toViewDto() = ViewAccountDto(
    id = id.toString(),
    name = name,
    currency = currency.currencyCode,
    type = type.name,
    balance = balance.toDecimal(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun Account.toBalanceViewDto() = ViewBalanceDto(
    accountId = id.toString(),
    currency = currency.currencyCode,
    amount = balance.toDecimal(),
)
