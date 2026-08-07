package altak.ledger.application.entry

import altak.ledger.domain.entry.EntryLine
import altak.ledger.domain.entry.JournalEntry

fun JournalEntry.toViewDto() = ViewEntryDto(
    id = id.toString(),
    description = description,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    lines = lines.map { it.toViewDto() },
)

fun EntryLine.toViewDto() = ViewEntryLineDto(
    accountId = accountId.toString(),
    direction = direction.name,
    amount = amount.toDecimal(),
)


