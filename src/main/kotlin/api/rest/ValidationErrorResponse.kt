package altak.ledger.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ValidationErrorResponse(
    val errors: List<String>,
)
