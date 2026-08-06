package altak.api.rest.accounts

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

// `@field:` is required: without the use-site target the annotations land on the constructor
// parameter, where Hibernate Validator cannot see them.
@Serializable
data class OpenAccountRequest(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 64, message = "must be at most 64 characters")
    val name: String,

    @field:Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 code")
    val currency: String,

    @field:PositiveOrZero(message = "must not be negative")
    val openingBalance: Long,
)

@Serializable
data class AccountResponse(
    val id: String,
    val name: String,
    val currency: String,
    val balance: Long,
)
