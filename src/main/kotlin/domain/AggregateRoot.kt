package altak.ledger.domain

import kotlin.time.Instant

interface AggregateRoot<ID> {

    val id: ID

    val createdAt: Instant

    val updatedAt: Instant
}
