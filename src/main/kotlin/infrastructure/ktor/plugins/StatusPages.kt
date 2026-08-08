package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.ErrorResponse
import altak.ledger.application.shared.MalformedValue
import altak.ledger.application.shared.StatusCode
import altak.ledger.application.shared.UseCaseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
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

        // A use case is trusted to explain itself, but only once it has named its answer: an
        // unannotated one is as much a mistake of this code as anything caught below.
        exception<UseCaseException> { call, cause ->
            when (val answer = cause.statusCode()) {
                null -> call.respond(HttpStatusCode.InternalServerError, unexpected())
                else -> call.respond(answer, cause.asResponse())
            }
        }
    }
}

private fun unexpected() = ErrorResponse(listOf(UNEXPECTED_FAILURE))

private fun UseCaseException.statusCode() =
    javaClass.getAnnotation(StatusCode::class.java)
        ?.let { HttpStatusCode.fromValue(it.value) }

// kotlinx echoes the whole input after the reason; the caller sent it, so only the reason is news.
private fun Throwable.asResponse() = ErrorResponse(listOfNotNull(message?.substringBefore("\nJSON input:")))

private fun Throwable.explained() =
    generateSequence(this) { it.cause }
        .firstOrNull { it is MalformedValue || it is IllegalArgumentException }
        ?.asResponse()
        ?: ErrorResponse(listOf("The request body could not be read"))
