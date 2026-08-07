package altak.ledger.application.shared

import altak.ledger.domain.Cursor
import jakarta.validation.constraints.Min
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CursorDto(
    val after: Uuid? = null,

    @field:Min(value = 1, message = "must be at least 1")
    val limit: Int = Cursor.DEFAULT_LIMIT,
) {
    fun <ID> toDomain(id: (Uuid) -> ID): Cursor<ID> = Cursor(after?.let(id), limit)
}
