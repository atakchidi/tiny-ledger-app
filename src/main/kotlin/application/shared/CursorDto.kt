package altak.ledger.application.shared

import altak.ledger.domain.Cursor
import jakarta.validation.constraints.Min
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CursorDto(
    @Serializable(with = UuidSerializer::class)
    @JsonSchema.Description("The id of the last record on the previous page")
    @JsonSchema.Example("\"019fdb85-c939-7780-9548-55fe6716fede\"")
    val after: Uuid? = null,

    @field:Min(value = 1, message = "must be at least 1")
    @JsonSchema.Description("How many records the page holds, at most ${Cursor.MAX_LIMIT}")
    @JsonSchema.Example("50")
    val limit: Int = Cursor.DEFAULT_LIMIT,
) {
    fun <ID> toDomain(id: (Uuid) -> ID): Cursor<ID> = Cursor(after?.let(id), limit)
}
