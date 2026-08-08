package altak.ledger.application.account

import altak.ledger.application.shared.CurrencySerializer
import altak.ledger.application.shared.UuidSerializer
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountType
import io.ktor.openapi.JsonSchema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import org.hibernate.validator.constraints.Length
import java.util.Currency
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class OpenAccountDto(
    @field:NotBlank
    @field:Size(max = 64)
    @JsonSchema.Description("What to call the account holder; free text, shown to humans")
    @JsonSchema.Example("Alice")
    val name: String,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency the account is held in, as an ISO 4217 code")
    @JsonSchema.Example("EUR")
    val currency: Currency,

    @field:Length(min=3, max = 32)
    @field:Pattern(
        regexp = AccountReference.FORMAT,
    )
    @JsonSchema.Description("The reference other systems quote this account by: 3 to 32 upper-case letters, digits or dashes.")
    @JsonSchema.Example("ACC-000123")
    val reference: String,
)

@Serializable
data class ViewAccountDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,

    @JsonSchema.Description("The reference this account is known by outside; either key addresses it")
    @JsonSchema.Example("ACC-000123")
    val reference: String,

    @JsonSchema.Description("What the account holder is called")
    @JsonSchema.Example("Alice")
    val name: String,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency the account is held in, as an ISO 4217 code")
    @JsonSchema.Example("EUR")
    val currency: Currency,

    val type: AccountType,

    @JsonSchema.Description("What the ledger owes the holder right now")
    @JsonSchema.Example("10.50")
    val balance: String,

    val createdAt: Instant,

    val updatedAt: Instant,
)
