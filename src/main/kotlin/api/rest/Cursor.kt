package altak.ledger.api.rest

import altak.ledger.application.shared.CursorDto
import altak.ledger.application.shared.MalformedValue
import altak.ledger.domain.Cursor
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.util.AttributeKey
import jakarta.validation.Validator
import kotlin.uuid.Uuid

class CursorResolutionConfig {
    lateinit var validator: Validator
}

val CursorResolution = createApplicationPlugin("CursorResolution", ::CursorResolutionConfig) {
    val validator = pluginConfig.validator

    on(CallSetup) { call ->
        call.attributes.put(CursorAttribute) { validator.validated(call.request.queryParameters.toCursorDto()) }
    }
}

val ApplicationCall.cursor: CursorDto get() = attributes[CursorAttribute]()

private val CursorAttribute = AttributeKey<() -> CursorDto>("cursor")

private fun Parameters.toCursorDto() = CursorDto(
    after = get("after")?.toCursorId(),
    limit = get("limit")?.toLimit() ?: Cursor.DEFAULT_LIMIT,
)

private fun Validator.validated(cursor: CursorDto): CursorDto {
    val violations = validate(cursor)

    if (violations.isNotEmpty()) {
        throw RequestValidationException(cursor, violations.map { "${it.propertyPath}: ${it.message}" }.sorted())
    }

    return cursor
}

private fun String.toCursorId(): Uuid =
    try {
        Uuid.parse(this)
    } catch (notAnId: IllegalArgumentException) {
        throw MalformedValue(this, "a cursor")
    }

private fun String.toLimit(): Int = toIntOrNull() ?: throw MalformedValue(this, "a whole number")
