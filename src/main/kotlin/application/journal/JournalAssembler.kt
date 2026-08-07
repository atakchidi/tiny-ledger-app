package altak.ledger.application.journal

import altak.ledger.domain.journal.Balance
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.JournalEntry

fun JournalEntry.toViewDto() = ViewEntryDto(
    id = id.value,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt,
    totalDebit = debited.toDecimal(),
    totalCredit = credited.toDecimal(),
    lines = lines.map { it.toViewDto() },
)

fun EntryLine.toViewDto() = ViewEntryLineDto(
    accountId = accountId.toString(),
    direction = direction.name,
    amount = amount.toDecimal(),
)

fun Balance.toViewDto() = ViewBalanceDto(
    onDate = onDate,
    accountId = account.id.toString(),
    reference = account.reference.toString(),
    currency = account.currency,
    amount = amount.toDecimal(),
)
