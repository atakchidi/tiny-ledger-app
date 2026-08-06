package altak.ledger.application.account

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

// `@field:` is required: without the use-site target the annotations land on the constructor
// parameter, where Hibernate Validator cannot see them.
@Serializable
data class OpenAccountDto(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 64, message = "must be at most 64 characters")
    val name: String,

    @field:Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 code")
    val currency: String,
)

@Serializable
data class ViewAccountDto(
    val id: String,
    val name: String,
    val currency: String,
    val type: String,
    val balance: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ViewBalanceDto(
    val accountId: String,
    val currency: String,
    val amount: String,
)
