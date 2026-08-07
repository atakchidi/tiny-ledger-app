package altak.ledger.application.account

import altak.ledger.application.shared.BigDecimalSerializer
import altak.ledger.application.shared.CurrencySerializer
import altak.ledger.domain.account.AccountReference
import io.ktor.openapi.JsonSchema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Currency

// `@field:` is required: without the use-site target the annotations land on the constructor
// parameter, where Hibernate Validator cannot see them.
@Serializable
data class OpenAccountDto(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 64, message = "must be at most 64 characters")
    @JsonSchema.Description("What to call the account holder; free text, shown to humans")
    @JsonSchema.Example("\"Alice\"")
    val name: String,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency the account is held in, as an ISO 4217 code")
    @JsonSchema.Example("\"EUR\"")
    val currency: Currency,

    @field:Pattern(
        regexp = AccountReference.FORMAT,
        message = "must be 3 to 32 upper-case letters, digits or dashes",
    )
    @JsonSchema.Description(
        "The reference other systems quote this account by: 3 to 32 upper-case letters, digits " +
            "or dashes. The ledger issues one if you bring none.",
    )
    @JsonSchema.Example("\"ACC-000123\"")
    val reference: String? = null,
)

@Serializable
data class ViewAccountDto(
    @JsonSchema.Description("The id the ledger issued, a UUIDv7")
    @JsonSchema.Example("\"019fdb85-c939-7780-9548-55fe6716fede\"")
    val id: String,

    @JsonSchema.Description("The reference this account is known by outside; either key addresses it")
    @JsonSchema.Example("\"ACC-000123\"")
    val reference: String,

    @JsonSchema.Description("What the account holder is called")
    @JsonSchema.Example("\"Alice\"")
    val name: String,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency the account is held in, as an ISO 4217 code")
    @JsonSchema.Example("\"EUR\"")
    val currency: Currency,

    @JsonSchema.Description("Which side of the books the account sits on")
    @JsonSchema.Example("\"LIABILITY\"")
    val type: String,

    @Serializable(with = BigDecimalSerializer::class)
    @JsonSchema.Description(
        "What the account stands at, in the currency's own precision. A read model kept as " +
            "movements post; ask /balances for the journal's own answer.",
    )
    @JsonSchema.Example("74.50")
    val balance: BigDecimal,

    @JsonSchema.Description("When the account was opened")
    @JsonSchema.Example("\"2026-08-07T09:20:05.968677Z\"")
    val createdAt: String,

    @JsonSchema.Description("When a movement last touched it")
    @JsonSchema.Example("\"2026-08-07T09:20:06.104900Z\"")
    val updatedAt: String,
)
