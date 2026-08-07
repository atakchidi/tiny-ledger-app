package altak.ledger.application.entry

import altak.ledger.application.shared.BigDecimalSerializer
import io.ktor.openapi.JsonSchema
import altak.ledger.domain.entry.MovementType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class MovementDto(
    @JsonSchema.Description("Which way the money goes: a DEPOSIT pays in, a WITHDRAWAL takes out")
    @JsonSchema.Example("\"DEPOSIT\"")
    val type: MovementType,

    @field:DecimalMin(value = "0", inclusive = false, message = "must be a positive amount")
    @field:Digits(integer = 16, fraction = 4, message = "must be an amount a currency can hold")
    @Serializable(with = BigDecimalSerializer::class)
    @JsonSchema.Description(
        "How much to move, in the account's own currency and no finer than that currency allows",
    )
    @JsonSchema.Example("10.50")
    val amount: BigDecimal,

    @field:Size(max = 140, message = "must be at most 140 characters")
    @JsonSchema.Description("What the movement was for; the ledger names it Deposit or Withdrawal if you say nothing")
    @JsonSchema.Example("\"Salary\"")
    val description: String? = null,
)

@Serializable
data class ViewEntryDto(
    @JsonSchema.Description("The id of the journal entry, a UUIDv7")
    @JsonSchema.Example("\"019fdb85-c9b8-789a-a996-91e43f014dfd\"")
    val id: String,

    @JsonSchema.Description("What the movement was for")
    @JsonSchema.Example("\"Salary\"")
    val description: String,

    @JsonSchema.Description("When the entry was written to the journal")
    @JsonSchema.Example("\"2026-08-07T09:20:06.072617Z\"")
    val createdAt: String,

    @JsonSchema.Description("When it last changed; entries are never amended, so this matches createdAt")
    @JsonSchema.Example("\"2026-08-07T09:20:06.072617Z\"")
    val updatedAt: String,

    @JsonSchema.Description("Both sides of the entry: what was debited and what was credited, always equal")
    val lines: List<ViewEntryLineDto>,
)

@Serializable
data class ViewEntryLineDto(
    @JsonSchema.Description("The account this side of the entry lands on")
    @JsonSchema.Example("\"019fdb85-c939-7780-9548-55fe6716fede\"")
    val accountId: String,

    @JsonSchema.Description("Which side of the account the amount lands on")
    @JsonSchema.Example("\"CREDIT\"")
    val direction: String,

    @Serializable(with = BigDecimalSerializer::class)
    @JsonSchema.Description("How much this side moves, in the account's own currency")
    @JsonSchema.Example("10.50")
    val amount: BigDecimal,
)
