package altak.ledger.application.journal

import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.journal.Balance
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.JournalEntry

typealias References = (AccountId) -> AccountReference

fun JournalEntry.toViewDto(references: References) = ViewEntryDto(
    id = id.value,
    description = description,
    occurredOn = occurredOn,
    createdAt = createdAt,
    updatedAt = updatedAt,
    currency = currency,
    totalDebit = debited.toPlainString(),
    totalCredit = credited.toPlainString(),
    lines = lines.map { it.toViewDto(references) },
)

fun EntryLine.toViewDto(references: References) = ViewEntryLineDto(
    accountId = accountId.toString(),
    reference = references(accountId).toString(),
    direction = direction.name,
    amount = amount.toPlainString(),
)

fun Balance.toViewDto() = ViewBalanceDto(
    onDate = onDate,
    accountId = account.id.toString(),
    reference = account.reference.toString(),
    currency = account.currency,
    amount = amount.toPlainString(),
)
