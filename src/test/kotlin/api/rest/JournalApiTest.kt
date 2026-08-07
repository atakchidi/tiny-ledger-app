package altak.ledger.api.rest

import altak.ledger.infrastructure.ktor.rootModule
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JournalApiTest {

    private fun ApplicationTestBuilder.startServer() = application { rootModule() }

    private suspend fun HttpClient.openAccount(reference: String = "ACC-ALICE") =
        post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","currency":"EUR","reference":"$reference"}""")
        }

    private suspend fun HttpClient.record(
        account: String = "ACC-ALICE",
        type: String = "DEPOSIT",
        amount: String = "10.00",
        description: String? = null,
    ) = post("/journal/entries") {
        contentType(ContentType.Application.Json)
        setBody(
            description
                ?.let { """{"account":"$account","type":"$type","amount":$amount,"description":"$it"}""" }
                ?: """{"account":"$account","type":"$type","amount":$amount}""",
        )
    }

    private suspend fun HttpClient.entriesOf(account: String = "ACC-ALICE", query: String = "") =
        get("/journal/entries?account=$account$query")

    private suspend fun HttpClient.balances(query: String = "") = get("/journal/balances$query")

    private suspend fun HttpResponse.amountOf(reference: String) =
        records()
            .map { it.jsonObject }
            .single { it.getValue("reference").jsonPrimitive.content == reference }
            .getValue("amount")
            .jsonPrimitive
            .content

    @Test
    fun `records a deposit as a balanced entry`() = testApplication {
        startServer()
        val alice = client.openAccount().id()

        val response = client.record(amount = "10.50")

        assertEquals(HttpStatusCode.Created, response.status)
        with(response.bodyAsText()) {
            assertContains(this, """"description":"Deposit"""")
            assertContains(this, """"direction":"CREDIT"""")
            assertContains(this, """"direction":"DEBIT"""")
            assertContains(this, """"amount":10.50""")
            assertContains(this, """"totalDebit":10.50""")
            assertContains(this, """"totalCredit":10.50""")
        }
        assertEquals("10.50", client.balances("?account=$alice").amountOf("ACC-ALICE"))
    }

    @Test
    fun `a deposit then a withdrawal leave the balance in between`() = testApplication {
        startServer()
        client.openAccount()

        client.record(amount = "10.50")
        client.record(type = "WITHDRAWAL", amount = "4.00", description = "Rent")

        assertEquals("6.50", client.balances().amountOf("ACC-ALICE"))
    }

    @Test
    fun `lets a holder overdraw`() = testApplication {
        startServer()
        client.openAccount()

        client.record(type = "WITHDRAWAL", amount = "2.50")

        assertEquals("-2.50", client.balances().amountOf("ACC-ALICE"))
    }

    @Test
    fun `keeps the cash the ledger holds equal to what it owes`() = testApplication {
        startServer()
        client.openAccount()
        client.openAccount(reference = "ACC-BOB")

        client.record(amount = "10.00")
        client.record(account = "ACC-BOB", amount = "2.50")
        client.record(type = "WITHDRAWAL", amount = "4.00")

        val balances = client.balances()
        val owed = balances.records()
            .map { it.jsonObject }
            .filter { it.getValue("reference").jsonPrimitive.content.startsWith("ACC-") }
            .sumOf { it.getValue("amount").jsonPrimitive.content.toBigDecimal() }

        assertEquals("8.50", balances.amountOf("CASH-EUR"))
        assertEquals("8.50", owed.toPlainString())
    }

    @Test
    fun `shows the entries of an account`() = testApplication {
        startServer()
        client.openAccount()
        val deposit = client.record(amount = "10.00").id()
        val withdrawal = client.record(type = "WITHDRAWAL", amount = "4.00").id()

        val entries = client.entriesOf()

        assertEquals(
            listOf(deposit, withdrawal),
            entries.records().map { it.jsonObject.getValue("id").jsonPrimitive.content },
        )
        assertNull(entries.nextCursor())
    }

    @Test
    fun `walks the entries a page at a time`() = testApplication {
        startServer()
        client.openAccount()
        val movements = (1..3).map { client.record(amount = "$it.00").id() }

        val firstPage = client.entriesOf(query = "&limit=2")
        val cursor = firstPage.nextCursor()
        val secondPage = client.entriesOf(query = "&limit=2&after=$cursor")

        assertEquals(movements.take(2), firstPage.records().map { it.jsonObject.getValue("id").jsonPrimitive.content })
        assertEquals(movements.drop(2), secondPage.records().map { it.jsonObject.getValue("id").jsonPrimitive.content })
        assertEquals(movements[1], cursor)
    }

    @Test
    fun `reads the journal as it stood on a date`() = testApplication {
        startServer()
        client.openAccount()
        client.record(amount = "10.00")

        val before = client.balances("?account=ACC-ALICE&onDate=2020-01-01T00:00:00Z")
        val now = client.balances("?account=ACC-ALICE")

        assertEquals("0.00", before.amountOf("ACC-ALICE"))
        assertEquals("10.00", now.amountOf("ACC-ALICE"))
        assertEquals("2020-01-01T00:00:00Z", before.records().single().jsonObject.getValue("onDate").jsonPrimitive.content)
    }

    @Test
    fun `answers for every account when none is named`() = testApplication {
        startServer()
        client.openAccount()
        client.openAccount(reference = "ACC-BOB")
        client.record(amount = "10.00")
        client.record(account = "ACC-BOB", amount = "2.50")

        val balances = client.balances()

        assertEquals(
            setOf("ACC-ALICE", "ACC-BOB", "CASH-EUR"),
            balances.records().map { it.jsonObject.getValue("reference").jsonPrimitive.content }.toSet(),
        )
    }

    @Test
    fun `refuses an amount the currency cannot hold`() = testApplication {
        startServer()
        client.openAccount()

        val tooPrecise = client.record(amount = "10.505")
        val notANumber = client.record(amount = "\"ten\"")
        val nothing = client.record(amount = "0.00")

        assertEquals(HttpStatusCode.BadRequest, tooPrecise.status)
        assertContains(tooPrecise.bodyAsText(), "decimal places")
        assertEquals(HttpStatusCode.BadRequest, notANumber.status)
        assertContains(notANumber.bodyAsText(), """\"ten\" is not a decimal number""")
        assertEquals(HttpStatusCode.BadRequest, nothing.status)
        assertContains(nothing.bodyAsText(), "must be a positive amount")
    }

    @Test
    fun `refuses a page nobody could fill`() = testApplication {
        startServer()
        client.openAccount()

        val empty = client.entriesOf(query = "&limit=0")
        val beyondTheLedger = client.entriesOf(query = "&limit=500")

        assertEquals(HttpStatusCode.BadRequest, empty.status)
        assertContains(empty.bodyAsText(), "limit")
        assertEquals(HttpStatusCode.BadRequest, beyondTheLedger.status)
        assertContains(beyondTheLedger.bodyAsText(), "limit")
    }

    @Test
    fun `refuses a cursor and a date it cannot read`() = testApplication {
        startServer()
        client.openAccount()

        val cursor = client.entriesOf(query = "&after=not-an-entry")
        val date = client.balances("?onDate=yesterday")

        assertEquals(HttpStatusCode.BadRequest, cursor.status)
        assertContains(cursor.bodyAsText(), "is not an identifier")
        assertEquals(HttpStatusCode.BadRequest, date.status)
        assertContains(date.bodyAsText(), "is not an ISO-8601 moment")
    }

    @Test
    fun `has nothing to move on an account it does not keep`() = testApplication {
        startServer()

        val recorded = client.record(account = "ACC-NOBODY")
        val listed = client.entriesOf(account = "ACC-NOBODY")
        val balanced = client.balances("?account=ACC-NOBODY")

        assertEquals(HttpStatusCode.NotFound, recorded.status)
        assertEquals(HttpStatusCode.NotFound, listed.status)
        assertEquals(HttpStatusCode.NotFound, balanced.status)
    }
}
