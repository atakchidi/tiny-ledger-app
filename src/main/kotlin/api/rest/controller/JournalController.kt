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
import altak.ledger.api.rest.schemaOf
import altak.ledger.application.journal.BalanceQueryDto
import altak.ledger.application.journal.ViewBalanceDto
import altak.ledger.application.journal.service.ListBalances
import altak.ledger.application.journal.service.ListBalancesService
import altak.ledger.application.journal.EntryQueryDto
import altak.ledger.application.journal.RecordAccountEntryDto
import altak.ledger.application.journal.ViewEntryDto
import altak.ledger.application.journal.service.ListAccountEntries
import altak.ledger.application.journal.service.ListAccountEntriesService
import altak.ledger.application.journal.service.RecordAccountEntry
import altak.ledger.application.journal.service.RecordAccountEntryService
import altak.ledger.application.shared.CursorDto
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi

val journalController = RestController {
    route("/journal") {

        get("/entries") {
            val service = inject<ListAccountEntriesService>()
            val entries = ListAccountEntries(call.receiveQuery<EntryQueryDto>(), call.receiveQuery<CursorDto>())

            call.respond(
                service.execute(entries).asApiResponse()
            )
        }.describe {
            operationId = "listAccountEntries"
            summary = "List the entries of an account"
            description = "The movements the account took part in, oldest first, a page at a time. " +
                "Pass the `nextCursor` of a page back as `after` to read the one behind it."
            tag("journal")
            pages("entries")

            parameters {
                query("account") {
                    description = "The id of an account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
            }

            responses {
                answers<ApiResponse.Listing<ViewEntryDto>>(OK, "A page of the account's history")
                refuses(BadRequest, "The page asked for could not be filled")
                refuses(NotFound, "No account by that id")
            }
        }

        post<RecordAccountEntryDto>("/entries") { movement ->
            val service = inject<RecordAccountEntryService>()
            call.respond(
                Created,
                ApiResponse.View(service.execute(RecordAccountEntry(movement)))
            )
        }.describe {
            operationId = "recordAccountEntry"
            summary = "Record an entry against an account"
            description = "A deposit raises what the ledger owes the holder and the cash it holds alike; " +
                "a withdrawal lowers both. The movement names which."
            tag("journal")
            accepts<RecordAccountEntryDto>()

            responses {
                answers<ApiResponse.View<ViewEntryDto>>(Created, "The entry the movement was recorded as")
                refuses(BadRequest, "The amount was not one the account could take")
                refuses(NotFound, "No account by that id")
            }
        }

        get("/balances") {
            val service = inject<ListBalancesService>()
            val balances = ListBalances(call.receiveQuery<BalanceQueryDto>(), call.receiveQuery<CursorDto>())

            call.respond(
                service.execute(balances).asApiResponse()
            )
        }.describe {
            operationId = "listBalances"
            summary = "List balances"
            description = "What the ledger owes, and holds, as the journal stood on a date. " +
                "Without an account it answers for every account it keeps; without a date, for right now."
            tag("journal")
            pages("balances")

            parameters {
                query("account") {
                    description = "The id of an account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
                query("onDate") {
                    description = "The moment to read the journal at, ISO-8601; defaults to now"
                    schema = schemaOf<String>()
                }
            }

            responses {
                answers<ApiResponse.Listing<ViewBalanceDto>>(OK, "The balances as the journal stood")
                refuses(BadRequest, "The query could not be read")
                refuses(NotFound, "No account by that id")
            }
        }
    }
}
