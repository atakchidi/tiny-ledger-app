package altak.ledger.application.shared

import altak.ledger.domain.Cursor
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CursorDto(
    @Serializable(with = UuidSerializer::class)
    val after: Uuid? = null,

    @field:Min(value = 1)
    @field:Max(value = Cursor.MAX_LIMIT.toLong())
    val limit: Int = DEFAULT_LIMIT,
) {
    fun <ID> toDomain(id: (Uuid) -> ID): Cursor<ID> = Cursor(limit, after?.let(id))

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
