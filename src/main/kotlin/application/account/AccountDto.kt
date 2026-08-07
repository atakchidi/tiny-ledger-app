package altak.ledger.application.account

import altak.ledger.application.shared.BigDecimalSerializer
import altak.ledger.application.shared.CurrencySerializer
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
    val name: String,

    @Serializable(with = CurrencySerializer::class)
    val currency: Currency,

    @field:Pattern(
        regexp = "[A-Za-z0-9][A-Za-z0-9-]{2,31}",
        message = "must be 3 to 32 letters, digits or dashes",
    )
    val reference: String? = null,
)

@Serializable
data class ViewAccountDto(
    val id: String,
    val reference: String,
    val name: String,
    @Serializable(with = CurrencySerializer::class)
    val currency: Currency,
    val type: String,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ViewAccountsDto(
    val accounts: List<ViewAccountDto>,
    val nextCursor: String? = null,
)

@Serializable
data class ViewBalanceDto(
    val accountId: String,
    @Serializable(with = CurrencySerializer::class)
    val currency: Currency,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
)
