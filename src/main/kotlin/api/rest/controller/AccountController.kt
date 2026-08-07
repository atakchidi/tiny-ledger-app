@file:OptIn(ExperimentalKtorApi::class)

package altak.ledger.api.rest.controller

import altak.ledger.api.rest.ApiResponse
import altak.ledger.api.rest.accepts
import altak.ledger.api.rest.answers
import altak.ledger.api.rest.asApiResponse
import altak.ledger.api.rest.inject
import altak.ledger.api.rest.pages
import altak.ledger.api.rest.receiveQuery
import altak.ledger.api.rest.refuses
import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.service.ListAccounts
import altak.ledger.application.shared.CursorDto
import altak.ledger.application.account.service.ListAccountsService
import altak.ledger.application.account.service.OpenAccountService
import altak.ledger.application.account.service.ViewAccount
import altak.ledger.application.account.service.ViewAccountService
import altak.ledger.application.entry.MovementDto
import altak.ledger.application.entry.ViewEntryDto
import altak.ledger.application.entry.service.RecordAccountEntry
import altak.ledger.application.entry.service.RecordAccountEntryService
import altak.ledger.application.entry.service.ListAccountEntries
import altak.ledger.application.entry.service.ListAccountEntriesService
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
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
            call.respond(Created, ApiResponse.View(inject<OpenAccountService>().execute(account)))
        }.describe {
            operationId = "openAccount"
            summary = "Open an account"
            description = "Opens an account for a holder. What the ledger owes them starts at zero."
            tag("accounts")
            accepts<OpenAccountDto>()

            responses {
                answers<ApiResponse.View<ViewAccountDto>>(Created, "The account that was opened")
                refuses(BadRequest, "The request violated a validation constraint")
                refuses(Conflict, "An account is already open under that reference")
            }
        }

        get {
            call.respond(inject<ListAccountsService>().execute(ListAccounts(call.receiveQuery<CursorDto>())).asApiResponse())
        }.describe {
            operationId = "listAccounts"
            summary = "List accounts"
            description = "Every account the ledger keeps, the cash accounts behind the holders included."
            tag("accounts")
            pages("accounts")

            responses {
                answers<ApiResponse.Listing<ViewAccountDto>>(OK, "A page of the accounts on the books")
                refuses(BadRequest, "The page asked for could not be filled")
            }
        }

        route("/{id}") {
            get {
                call.respond(ApiResponse.View(inject<ViewAccountService>().execute(ViewAccount(call.pathParameters.getOrFail("id")))))
            }.describe {
                operationId = "viewAccount"
                summary = "View an account"
                description = "The account as it stands, found by its id or by the reference it is known by outside."
                tag("accounts")

                responses {
                    answers<ApiResponse.View<ViewAccountDto>>(OK, "The account as it stands")
                    refuses(NotFound, "No account by that id")
                }
            }

            get("/entries") {
                call.respond(
                    inject<ListAccountEntriesService>().execute(ListAccountEntries(call.pathParameters.getOrFail("id"), call.receiveQuery<CursorDto>())).asApiResponse()
                )
            }.describe {
                operationId = "listAccountEntries"
                summary = "List the entries of an account"
                description = "The movements the account took part in, oldest first, a page at a time. " +
                    "Pass the `nextCursor` of a page back as `after` to read the one behind it."
                tag("ledger")
                pages("entries")

                responses {
                    answers<ApiResponse.Listing<ViewEntryDto>>(OK, "A page of the account's history")
                    refuses(BadRequest, "The page asked for could not be filled")
                    refuses(NotFound, "No account by that id")
                }
            }

            post<MovementDto>("/entries") { movement ->
                val entry = inject<RecordAccountEntryService>().execute(
                    RecordAccountEntry(call.pathParameters.getOrFail("id"), movement),
                )

                call.respond(Created, ApiResponse.View(entry))
            }.describe {
                operationId = "recordAccountEntry"
                summary = "Record an entry against an account"
                description = "A deposit raises what the ledger owes the holder and the cash it holds alike; " +
                    "a withdrawal lowers both. The movement names which."
                tag("ledger")
                accepts<MovementDto>()

                responses {
                    answers<ApiResponse.View<ViewEntryDto>>(Created, "The entry the movement was recorded as")
                    refuses(BadRequest, "The amount was not one the account could take")
                    refuses(NotFound, "No account by that id")
                }
            }
        }
    }
}
