package altak.ledger.infrastructure.validation

import altak.ledger.ZONE
import altak.ledger.application.journal.RecordAccountEntryDto
import altak.ledger.domain.journal.MovementType
import altak.ledger.fixedClock
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ValidatorsTest {

    // Late enough in the day that the ledger's zone is already on the next date, which is where a
    // validator running off the machine's own clock would disagree with the books.
    private val lateEvening = Instant.parse("2026-08-06T22:30:00Z")

    private val validator = validatorFactory(fixedClock(lateEvening), ZONE).validator

    private fun violationsFor(occurredOn: String) =
        validator.validate(
            RecordAccountEntryDto("ACC-ALICE", MovementType.DEPOSIT, BigDecimal("1.00"), occurredOn = LocalDate.parse(occurredOn)),
        ).map { "${it.propertyPath}: ${it.message}" }

    @Test
    fun `dates a temporal constraint by the ledger's own calendar`() {
        assertEquals(emptyList(), violationsFor("2026-08-07"))
        assertEquals(emptyList(), violationsFor("2026-05-02"))
    }

    @Test
    fun `refuses a date the ledger has not reached`() {
        assertEquals(
            listOf("occurredOn: must be a date in the past or in the present"),
            violationsFor("2026-08-08"),
        )
    }
}
