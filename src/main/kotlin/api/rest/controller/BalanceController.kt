@file:OptIn(ExperimentalKtorApi::class)

package altak.ledger.api.rest.controller

import altak.ledger.api.rest.ApiResponse
import altak.ledger.api.rest.answers
import altak.ledger.api.rest.asApiResponse
import altak.ledger.api.rest.inject
import altak.ledger.api.rest.pages
import altak.ledger.api.rest.receiveQuery
import altak.ledger.api.rest.refuses
import altak.ledger.api.rest.schemaOf
import altak.ledger.application.balance.BalanceQueryDto
import altak.ledger.application.balance.ViewBalanceDto
import altak.ledger.application.balance.service.ListBalances
import altak.ledger.application.balance.service.ListBalancesService
import altak.ledger.application.shared.CursorDto
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

val balanceController = RestController {
    get("/balances") {
        val balances = ListBalances(call.receiveQuery<BalanceQueryDto>(), call.receiveQuery<CursorDto>())

        call.respond(inject<ListBalancesService>().execute(balances).asApiResponse())
    }.describe {
        operationId = "listBalances"
        summary = "List balances"
        description = "What the ledger owes, and holds, as the journal stood on a date. " +
            "Without an account it answers for every account it keeps; without a date, for right now."
        tag("balances")
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
