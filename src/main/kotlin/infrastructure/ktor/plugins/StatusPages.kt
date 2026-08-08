package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.ErrorResponse
import altak.ledger.application.shared.MalformedValue
import altak.ledger.application.shared.StatusCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

private const val UNEXPECTED_FAILURE = "The ledger could not complete the request"

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.reasons))
        }

        exception<MalformedValue> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.asResponse())
        }

        exception<SerializationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.explained())
        }

        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.explained())
        }

        // An application exception names its own answer and is trusted to explain itself; anything
        // else broke an invariant, which is a bug here rather than a mistake by the caller.
        exception<RuntimeException> { call, cause ->
            val answer = cause.statusCode()

            call.application.log.error("${call.request.uri} answered ${answer ?: HttpStatusCode.InternalServerError}", cause)

            when (answer) {
                null -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse(listOf(UNEXPECTED_FAILURE)))
                else -> call.respond(answer, cause.asResponse())
            }
        }
    }
}

private fun RuntimeException.statusCode() =
    javaClass.getAnnotation(StatusCode::class.java)
        ?.let { HttpStatusCode.fromValue(it.value) }

// kotlinx echoes the whole input after the reason; the caller sent it, so only the reason is news.
private fun Throwable.asResponse() = ErrorResponse(listOfNotNull(message?.substringBefore("\nJSON input:")))

private fun Throwable.explained() =
    generateSequence(this) { it.cause }
        .firstOrNull { it is MalformedValue || it is IllegalArgumentException }
        ?.asResponse()
        ?: ErrorResponse(listOf("The request body could not be read"))
