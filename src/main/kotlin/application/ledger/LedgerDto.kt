package altak.ledger.application.ledger

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

@Serializable
data class MovementDto(
    @field:Pattern(regexp = "\\d+(\\.\\d+)?", message = "must be a positive decimal amount")
    val amount: String,

    @field:Size(max = 140, message = "must be at most 140 characters")
    val description: String? = null,
)

@Serializable
data class ViewEntryDto(
    val id: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val lines: List<ViewEntryLineDto>,
)

@Serializable
data class ViewEntryLineDto(
    val accountId: String,
    val direction: String,
    val amount: String,
)

@Serializable
data class ViewHistoryDto(
    val entries: List<ViewEntryDto>,
    val nextCursor: String? = null,
)
