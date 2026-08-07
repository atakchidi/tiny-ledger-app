@file:OptIn(ExperimentalKtorApi::class)

package altak.ledger.api.rest.accounts

import altak.ledger.api.rest.ErrorResponse
import altak.ledger.api.rest.RestController
import altak.ledger.api.rest.cursor
import altak.ledger.api.rest.inject
import altak.ledger.api.rest.schemaOf
import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.ViewAccountsDto
import altak.ledger.application.account.ViewBalanceDto
import altak.ledger.application.account.service.ListAccounts
import altak.ledger.application.account.service.ListAccountsService
import altak.ledger.application.account.service.OpenAccountService
import altak.ledger.application.account.service.ViewAccount
import altak.ledger.application.account.service.ViewAccountService
import altak.ledger.application.account.service.ViewBalance
import altak.ledger.application.account.service.ViewBalanceService
import altak.ledger.domain.Cursor
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.utils.io.ExperimentalKtorApi

val accountController = RestController {
    route("/accounts") {
        post<OpenAccountDto> { account ->
            call.respond(HttpStatusCode.Created, inject<OpenAccountService>().execute(account))
        }.describe {
            summary = "Open an account"
            description = "Opens an account for a holder. What the ledger owes them starts at zero."
            tag("accounts")

            requestBody {
                required = true
                schema = schemaOf<OpenAccountDto>()
            }

            responses {
                response(HttpStatusCode.Created.value) {
                    description = "The account that was opened"
                    schema = schemaOf<ViewAccountDto>()
                }
                response(HttpStatusCode.BadRequest.value) {
                    description = "The request violated a validation constraint"
                    schema = schemaOf<ErrorResponse>()
                }
                response(HttpStatusCode.Conflict.value) {
                    description = "An account is already open under that reference"
                    schema = schemaOf<ErrorResponse>()
                }
            }
        }

        get {
            call.respond(inject<ListAccountsService>().execute(ListAccounts(call.cursor)))
        }.describe {
            summary = "List accounts"
            description = "Every account the ledger keeps, the cash accounts behind the holders included."
            tag("accounts")

            parameters {
                query("after") {
                    description = "The id of the last record on the previous page"
                    schema = schemaOf<String>()
                }
                query("limit") {
                    description = "How many records the page holds, at most ${Cursor.MAX_LIMIT}"
                    schema = schemaOf<Int>()
                }
            }

            responses {
                response(HttpStatusCode.OK.value) {
                    description = "A page of the accounts on the books"
                    schema = schemaOf<ViewAccountsDto>()
                }
            }
        }

        get("/{id}") {
            call.respond(inject<ViewAccountService>().execute(ViewAccount(call.parameters.getOrFail("id"))))
        }.describe {
            summary = "View an account"
            description = "The account as it stands, found by its id or by the reference it is known by outside."
            tag("accounts")

            parameters {
                path("id") {
                    description = "The id of the account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
            }

            responses {
                response(HttpStatusCode.OK.value) {
                    description = "The account as it stands"
                    schema = schemaOf<ViewAccountDto>()
                }
                response(HttpStatusCode.NotFound.value) {
                    description = "No account by that id"
                    schema = schemaOf<ErrorResponse>()
                }
            }
        }

        get("/{id}/balance") {
            call.respond(inject<ViewBalanceService>().execute(ViewBalance(call.parameters.getOrFail("id"))))
        }.describe {
            summary = "View a balance"
            description = "What the ledger owes the holder right now."
            tag("accounts")

            parameters {
                path("id") {
                    description = "The id of the account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
            }

            responses {
                response(HttpStatusCode.OK.value) {
                    description = "The balance of the account"
                    schema = schemaOf<ViewBalanceDto>()
                }
                response(HttpStatusCode.NotFound.value) {
                    description = "No account by that id"
                    schema = schemaOf<ErrorResponse>()
                }
            }
        }
    }
}
