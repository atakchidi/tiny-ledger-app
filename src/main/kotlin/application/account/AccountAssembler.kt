package altak.ledger.application.account

import altak.ledger.domain.Cursor
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
    reference = reference.toString(),
    name = name,
    currency = currency,
    type = type.name,
    balance = balance.toDecimal(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun Account.toBalanceViewDto() = ViewBalanceDto(
    accountId = id.toString(),
    currency = currency,
    amount = balance.toDecimal(),
)

fun List<Account>.toViewDto(cursor: Cursor<AccountId>) = ViewAccountsDto(
    accounts = map { it.toViewDto() },
    nextCursor = lastOrNull()?.id?.toString()?.takeIf { size == cursor.limit },
)
