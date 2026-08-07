package altak.ledger.infrastructure

import altak.ledger.domain.IdGenerator
import kotlin.time.Clock
import kotlin.uuid.Uuid

class UuidV7Generator: IdGenerator {
    override fun nextId(clock: Clock) = Uuid.generateV7NonMonotonicAt(clock.now())
}
