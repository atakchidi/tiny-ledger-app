package altak.ledger.application.ledger

import altak.ledger.domain.Cursor
import altak.ledger.domain.ledger.EntryId
import altak.ledger.domain.ledger.EntryLine
import altak.ledger.domain.ledger.JournalEntry

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

fun List<JournalEntry>.toHistoryViewDto(cursor: Cursor<EntryId>) = ViewHistoryDto(
    entries = map { it.toViewDto() },
    nextCursor = lastOrNull()?.id?.toString()?.takeIf { size == cursor.limit },
)
