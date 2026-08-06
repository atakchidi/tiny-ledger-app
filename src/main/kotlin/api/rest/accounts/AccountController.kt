@file:OptIn(ExperimentalKtorApi::class)

package altak.api.rest.accounts

import altak.api.rest.RestController
import altak.api.rest.ValidationErrorResponse
import altak.api.rest.schemaOf
import io.ktor.http.*
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import java.util.UUID

val accountController = RestController {
    route("/accounts") {
        post<OpenAccountRequest> { request ->
            call.respond(
                HttpStatusCode.Created,
                AccountResponse(
                    id = UUID.randomUUID().toString(),
                    name = request.name,
                    currency = request.currency,
                    balance = request.openingBalance,
                ),
            )
        }.describe {
            summary = "Open an account"
            tag("accounts")

            requestBody {
                required = true
                schema = schemaOf<OpenAccountRequest>()
            }

            responses {
                response(HttpStatusCode.Created.value) {
                    description = "The account that was opened"
                    schema = schemaOf<AccountResponse>()
                }
                response(HttpStatusCode.BadRequest.value) {
                    description = "The request violated a validation constraint"
                    schema = schemaOf<ValidationErrorResponse>()
                }
            }
        }
    }
}
