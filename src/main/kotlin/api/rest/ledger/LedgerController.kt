@file:OptIn(ExperimentalKtorApi::class)

package altak.ledger.api.rest.ledger

import altak.ledger.api.rest.ErrorResponse
import altak.ledger.api.rest.RestController
import altak.ledger.api.rest.cursor
import altak.ledger.api.rest.inject
import altak.ledger.api.rest.schemaOf
import altak.ledger.application.ledger.MovementDto
import altak.ledger.application.ledger.ViewEntryDto
import altak.ledger.application.ledger.ViewHistoryDto
import altak.ledger.application.ledger.service.Deposit
import altak.ledger.application.ledger.service.DepositService
import altak.ledger.application.ledger.service.ViewHistory
import altak.ledger.application.ledger.service.ViewHistoryService
import altak.ledger.application.ledger.service.Withdraw
import altak.ledger.application.ledger.service.WithdrawService
import altak.ledger.domain.Cursor
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.utils.io.ExperimentalKtorApi

val ledgerController = RestController {
    route("/accounts/{id}") {
        post<MovementDto>("/deposits") { movement ->
            val entry = inject<DepositService>().execute(Deposit(call.parameters.getOrFail("id"), movement))

            call.respond(HttpStatusCode.Created, entry)
        }.describe {
            summary = "Deposit into an account"
            description = "Raises what the ledger owes the holder and the cash it holds alike."
            tag("ledger")

            parameters {
                path("id") {
                    description = "The id of the account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
            }

            requestBody {
                required = true
                schema = schemaOf<MovementDto>()
            }

            responses {
                response(HttpStatusCode.Created.value) {
                    description = "The entry the deposit was recorded as"
                    schema = schemaOf<ViewEntryDto>()
                }
                response(HttpStatusCode.BadRequest.value) {
                    description = "The amount was not one the account could take"
                    schema = schemaOf<ErrorResponse>()
                }
                response(HttpStatusCode.NotFound.value) {
                    description = "No account by that id"
                    schema = schemaOf<ErrorResponse>()
                }
            }
        }

        post<MovementDto>("/withdrawals") { movement ->
            val entry = inject<WithdrawService>().execute(Withdraw(call.parameters.getOrFail("id"), movement))

            call.respond(HttpStatusCode.Created, entry)
        }.describe {
            summary = "Withdraw from an account"
            description = "Lowers what the ledger owes the holder and the cash it holds alike."
            tag("ledger")

            parameters {
                path("id") {
                    description = "The id of the account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
            }

            requestBody {
                required = true
                schema = schemaOf<MovementDto>()
            }

            responses {
                response(HttpStatusCode.Created.value) {
                    description = "The entry the withdrawal was recorded as"
                    schema = schemaOf<ViewEntryDto>()
                }
                response(HttpStatusCode.BadRequest.value) {
                    description = "The amount was not one the account could take"
                    schema = schemaOf<ErrorResponse>()
                }
                response(HttpStatusCode.NotFound.value) {
                    description = "No account by that id"
                    schema = schemaOf<ErrorResponse>()
                }
            }
        }

        get("/entries") {
            val history = ViewHistory(call.parameters.getOrFail("id"), call.cursor)

            call.respond(inject<ViewHistoryService>().execute(history))
        }.describe {
            summary = "View the history of an account"
            description = "The movements the account took part in, oldest first, a page at a time. " +
                "Pass the `nextCursor` of a page back as `after` to read the one behind it."
            tag("ledger")

            parameters {
                path("id") {
                    description = "The id of the account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
                query("after") {
                    description = "The id of the last entry on the previous page"
                    schema = schemaOf<String>()
                }
                query("limit") {
                    description = "How many entries the page holds, at most ${Cursor.MAX_LIMIT}"
                    schema = schemaOf<Int>()
                }
            }

            responses {
                response(HttpStatusCode.OK.value) {
                    description = "A page of the account's history"
                    schema = schemaOf<ViewHistoryDto>()
                }
                response(HttpStatusCode.BadRequest.value) {
                    description = "The page asked for could not be filled"
                    schema = schemaOf<ErrorResponse>()
                }
                response(HttpStatusCode.NotFound.value) {
                    description = "No account by that id"
                    schema = schemaOf<ErrorResponse>()
                }
            }
        }
    }
}
