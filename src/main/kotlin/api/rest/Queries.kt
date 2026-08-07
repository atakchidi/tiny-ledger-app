package altak.ledger.api.rest

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import jakarta.validation.Validator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Reads the query string into [T], the way `receive` reads the body into a DTO — including the
 * Jakarta constraints the DTO carries, which the receive pipeline would otherwise be alone in
 * checking.
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveQuery(): T =
    queryFormat.decodeFromJsonElement<T>(request.queryParameters.asJson()).also { validate(it) }

@PublishedApi
internal val queryFormat = Json { isLenient = true; ignoreUnknownKeys = true }

@PublishedApi
internal fun Parameters.asJson() =
    JsonObject(entries().associate { (name, values) -> name to JsonPrimitive(values.first()) })

@PublishedApi
internal suspend fun ApplicationCall.validate(query: Any) {
    val violations = application.dependencies.resolve<Validator>().validate(query)

    if (violations.isNotEmpty()) {
        throw RequestValidationException(query, violations.map { "${it.propertyPath}: ${it.message}" }.sorted())
    }
}
