package altak.ledger.application.journal

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.journal.Balance
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.JournalEntry

typealias Accounts = (AccountId) -> Account

fun JournalEntry.toViewDto(accounts: Accounts) = ViewEntryDto(
    id = id.value,
    description = description,
    occurredOn = occurredOn,
    createdAt = createdAt,
    currency = currency,
    totalDebit = debited.toPlainString(),
    totalCredit = credited.toPlainString(),
    lines = lines.map { it.toViewDto(accounts) },
)

fun EntryLine.toViewDto(accounts: Accounts) = accounts(accountId).let { account ->
    ViewEntryLineDto(
        accountId = accountId.toString(),
        reference = account.reference.toString(),
        accountType = account.type,
        direction = direction.name,
        amount = amount.toPlainString(),
    )
}

fun Balance.toViewDto() = ViewBalanceDto(
    onDate = onDate,
    accountId = account.id.toString(),
    reference = account.reference.toString(),
    currency = account.currency,
    amount = amount.toPlainString(),
)
