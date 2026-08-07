package altak.ledger.api.rest

import altak.ledger.domain.Page
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

sealed interface ApiResponse<T> {

    val data: T

    @Serializable
    data class View<T>(
        override val data: T,
    ) : ApiResponse<T>

    @Serializable
    data class Listing<T>(
        override val data: List<T>,

        @JsonSchema.Description("Pass back as `after` to read the page behind this one; null on the last page")
        @JsonSchema.Example("\"019fdb85-c939-7780-9548-55fe6716fede\"")
        val nextCursor: String? = null,
    ) : ApiResponse<List<T>>
}

fun <T> Page<T>.asApiResponse() = ApiResponse.Listing(items, nextCursor)
