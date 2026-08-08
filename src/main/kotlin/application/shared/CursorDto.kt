package altak.ledger.application.shared

import altak.ledger.domain.Cursor
import altak.ledger.domain.Sorting
import io.ktor.openapi.JsonSchema
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

    @JsonSchema.Description("The field to order the records by; the id they were recorded under if left out")
    val sort: String? = null,

    @JsonSchema.Description("Which way to order them")
    val direction: Sorting.Direction = Sorting.Direction.ASC,
) {
    fun <ID> toDomain(id: (Uuid) -> ID): Cursor<ID> =
        Cursor(limit, after?.let(id), Sorting(sort ?: Sorting.ID, direction))

    companion object {
        const val DEFAULT_LIMIT = 20
    }
}
