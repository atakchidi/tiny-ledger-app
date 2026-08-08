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
import altak.ledger.api.rest.Sortable
import altak.ledger.api.rest.schemaOf
import altak.ledger.api.rest.sortedWithin
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

private val entriesSortableBy = Sortable("occurredOn")

private val balancesSortableBy = Sortable("reference")

val journalController = RestController {
    route("/journal") {

        get("/entries") {
            val service = inject<ListAccountEntriesService>()
            val page = call.receiveQuery<CursorDto>().sortedWithin(entriesSortableBy)
            val entries = ListAccountEntries(call.receiveQuery<EntryQueryDto>(), page)

            call.respond(
                service.execute(entries).asApiResponse()
            )
        }.describe {
            operationId = "listAccountEntries"
            summary = "List entries"
            description = "The movements the journal holds, in the order they were recorded, a page " +
                "at a time. With an account it lists only the entries that account took part in; " +
                "without one, every entry it keeps. Order them by the day they happened with " +
                "`sort=occurredOn`. Pass the `nextCursor` of a page back as `after` to read the one behind it."
            tag("journal")
            pages("entries", entriesSortableBy)

            parameters {
                query("account") {
                    description = "An account id or the reference it is known by outside; every entry if left out"
                    schema = schemaOf<String>()
                }
            }

            responses {
                answers<ApiResponse.Listing<ViewEntryDto>>(OK, "A page of the journal")
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
            description = "A movement posts two lines that balance: what the ledger owes the holder " +
                "and the cash it holds move together, and the type names which way they go."
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
            val page = call.receiveQuery<CursorDto>().sortedWithin(balancesSortableBy)
            val balances = ListBalances(call.receiveQuery<BalanceQueryDto>(), page)

            call.respond(
                service.execute(balances).asApiResponse()
            )
        }.describe {
            operationId = "listBalances"
            summary = "List balances"
            description = "What the ledger owes, and holds, as the journal stood on a date. " +
                "Without an account it answers for every account it keeps; without a date, for right now."
            tag("journal")
            pages("balances", balancesSortableBy)

            parameters {
                query("account") {
                    description = "The id of an account, or the reference it is known by outside"
                    schema = schemaOf<String>()
                }
                query("onDate") {
                    description = "The date to read the journal as of, as YYYY-MM-DD; defaults to today"
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
