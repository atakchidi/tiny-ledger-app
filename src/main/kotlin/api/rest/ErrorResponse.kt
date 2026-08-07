package altak.ledger.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val errors: List<String>,
)
