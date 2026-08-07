package altak.ledger.application.ledger

import altak.ledger.application.shared.BigDecimalSerializer
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class MovementDto(
    @field:DecimalMin(value = "0", inclusive = false, message = "must be a positive amount")
    @field:Digits(integer = 16, fraction = 4, message = "must be an amount a currency can hold")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,

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
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
)

@Serializable
data class ViewHistoryDto(
    val entries: List<ViewEntryDto>,
    val nextCursor: String? = null,
)
