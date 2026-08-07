package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.ErrorResponse
import altak.ledger.application.account.AccountAlreadyOpen
import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.shared.MalformedValue
import altak.ledger.domain.LedgerException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.reasons))
        }

        exception<AccountNotFound> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.asResponse())
        }

        exception<AccountAlreadyOpen> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.asResponse())
        }

        exception<LedgerException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.asResponse())
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
    }
}

// kotlinx echoes the whole input after the reason; the caller sent it, so only the reason is news.
private fun Throwable.asResponse() = ErrorResponse(listOfNotNull(message?.substringBefore("\nJSON input:")))

private fun Throwable.explained(): ErrorResponse =
    generateSequence(this) { it.cause }
        .firstOrNull { it is LedgerException || it is MalformedValue || it is IllegalArgumentException }
        ?.asResponse()
        ?: ErrorResponse(listOf("The request body could not be read"))
