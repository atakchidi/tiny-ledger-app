package altak.ledger.domain.journal

import altak.ledger.domain.IdGenerator
import altak.ledger.domain.LedgerCalendar
import altak.ledger.domain.LedgerException
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class JournalEntryFactory(
    private val ids: IdGenerator,
    private val calendar: LedgerCalendar,
    private val clock: Clock,
) {
    class FutureDated(message: String) : LedgerException(message)

    fun create(description: String, lines: List<EntryLine>, occurredOn: LocalDate? = null): JournalEntry {
        val today = calendar.today()
        val effectiveDate = occurredOn ?: today

        if (effectiveDate > today) {
            throw FutureDated("An entry cannot be dated $effectiveDate, which is after today, $today")
        }

        return JournalEntry(
            id = EntryId(ids.nextId(clock)),
            description = description,
            occurredOn = effectiveDate,
            createdAt = clock.now(),
            lines = lines,
        )
    }
}
