package altak.ledger.api.rest

import altak.ledger.infrastructure.ktor.rootModule
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LedgerApiTest {

    private fun ApplicationTestBuilder.startServer() = application { rootModule() }

    private suspend fun HttpClient.openAccount(currency: String = "EUR") =
        post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","currency":"$currency"}""")
        }.id()

    private suspend fun HttpClient.move(accountId: String, type: String, amount: String, description: String? = null) =
        post("/accounts/$accountId/entries") {
            contentType(ContentType.Application.Json)
            setBody(
                description
                    ?.let { """{"type":"$type","amount":"$amount","description":"$it"}""" }
                    ?: """{"type":"$type","amount":"$amount"}""",
            )
        }

    private suspend fun HttpClient.balanceOf(accountId: String, onDate: String = "") =
        Json.parseToJsonElement(get("/balances?account=$accountId$onDate").bodyAsText())
            .jsonObject
            .getValue("data")
            .jsonArray
            .single()
            .jsonObject
            .getValue("amount")
            .jsonPrimitive
            .content

    private suspend fun HttpClient.historyOf(accountId: String, query: String = "") =
        Json.parseToJsonElement(get("/accounts/$accountId/entries$query").bodyAsText()).jsonObject

    @Test
    fun `records a deposit as a balanced entry`() = testApplication {
        startServer()
        val alice = client.openAccount()

        val response = client.move(alice, "DEPOSIT", "10.50")

        assertEquals(HttpStatusCode.Created, response.status)
        with(response.bodyAsText()) {
            assertContains(this, """"description":"Deposit"""")
            assertContains(this, """"direction":"CREDIT"""")
            assertContains(this, """"direction":"DEBIT"""")
            assertContains(this, """"amount":10.50""")
        }
        assertEquals("10.50", client.balanceOf(alice))
    }

    @Test
    fun `a deposit then a withdrawal leave the balance in between`() = testApplication {
        startServer()
        val alice = client.openAccount()

        client.move(alice, "DEPOSIT", "10.50")
        client.move(alice, "WITHDRAWAL", "4.00", "Rent")

        assertEquals("6.50", client.balanceOf(alice))
    }

    @Test
    fun `lets a holder overdraw`() = testApplication {
        startServer()
        val alice = client.openAccount()

        client.move(alice, "WITHDRAWAL", "2.50")

        assertEquals("-2.50", client.balanceOf(alice))
    }

    @Test
    fun `keeps the cash the ledger holds equal to what it owes`() = testApplication {
        startServer()
        val alice = client.openAccount()
        val bob = client.openAccount()

        client.move(alice, "DEPOSIT", "10.00")
        client.move(bob, "DEPOSIT", "2.50")
        client.move(alice, "WITHDRAWAL", "4.00")

        val balances = Json.parseToJsonElement(client.get("/balances").bodyAsText())
            .jsonObject
            .getValue("data")
            .jsonArray
            .map { it.jsonObject }

        val cash = balances.single { it.getValue("reference").jsonPrimitive.content == "CASH-EUR" }
        val owed = balances.filter { it.getValue("reference").jsonPrimitive.content.startsWith("ACC-") }
            .sumOf { it.getValue("amount").jsonPrimitive.content.toBigDecimal() }

        assertEquals("8.50", cash.getValue("amount").jsonPrimitive.content)
        assertEquals("8.50", owed.toPlainString())
    }

    @Test
    fun `shows the history of an account`() = testApplication {
        startServer()
        val alice = client.openAccount()
        val deposit = client.move(alice, "DEPOSIT", "10.00").id()
        val withdrawal = client.move(alice, "WITHDRAWAL", "4.00").id()

        val history = client.historyOf(alice)

        assertEquals(
            listOf(deposit, withdrawal),
            history.getValue("data").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content },
        )
        assertNull(history["nextCursor"]?.jsonPrimitive?.contentOrNull())
    }

    @Test
    fun `walks the history a page at a time`() = testApplication {
        startServer()
        val alice = client.openAccount()
        val movements = (1..3).map { client.move(alice, "DEPOSIT", "$it.00").id() }

        val firstPage = client.historyOf(alice, "?limit=2")
        val cursor = firstPage.getValue("nextCursor").jsonPrimitive.content
        val secondPage = client.historyOf(alice, "?limit=2&after=$cursor")

        assertEquals(movements.take(2), firstPage.entryIds())
        assertEquals(movements.drop(2), secondPage.entryIds())
        assertEquals(movements[1], cursor)
    }

    @Test
    fun `refuses an amount the currency cannot hold`() = testApplication {
        startServer()
        val alice = client.openAccount()

        val tooPrecise = client.move(alice, "DEPOSIT", "10.505")
        val notANumber = client.move(alice, "DEPOSIT", "ten")
        val nothing = client.move(alice, "DEPOSIT", "0.00")

        assertEquals(HttpStatusCode.BadRequest, tooPrecise.status)
        assertContains(tooPrecise.bodyAsText(), "decimal places")
        assertEquals(HttpStatusCode.BadRequest, notANumber.status)
        assertContains(notANumber.bodyAsText(), """\"ten\" is not a decimal number""")
        assertEquals(HttpStatusCode.BadRequest, nothing.status)
        assertContains(nothing.bodyAsText(), "must be a positive amount")
    }

    @Test
    fun `refuses a cursor that could not have come from here`() = testApplication {
        startServer()
        val alice = client.openAccount()

        val notAnId = client.get("/accounts/$alice/entries?after=not-an-entry")
        val foreignId = client.get("/accounts/$alice/entries?after=8ac48f6e-0f4b-4d2f-9c1e-2b0a5f6d3c11")
        val notANumber = client.get("/accounts/$alice/entries?limit=lots")

        assertEquals(HttpStatusCode.BadRequest, notAnId.status)
        assertContains(notAnId.bodyAsText(), "is not an identifier")
        assertEquals(HttpStatusCode.BadRequest, foreignId.status)
        assertContains(foreignId.bodyAsText(), "is not an identifier this ledger issues")
        assertEquals(HttpStatusCode.BadRequest, notANumber.status)
        assertContains(notANumber.bodyAsText(), "limit")
        assertFalse(notANumber.bodyAsText().contains("JSON input"))
    }

    @Test
    fun `refuses a page nobody could fill`() = testApplication {
        startServer()
        val alice = client.openAccount()

        val empty = client.get("/accounts/$alice/entries?limit=0")
        val beyondTheLedger = client.get("/accounts/$alice/entries?limit=500")

        assertEquals(HttpStatusCode.BadRequest, empty.status)
        assertContains(empty.bodyAsText(), "limit: must be at least 1")
        assertEquals(HttpStatusCode.BadRequest, beyondTheLedger.status)
        assertContains(beyondTheLedger.bodyAsText(), "A page holds between 1 and 200 records")
    }

    @Test
    fun `has nothing to move on an account it does not keep`() = testApplication {
        startServer()

        val response = client.move("not-an-account", "DEPOSIT", "10.00")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "not found")
    }
}

private fun kotlinx.serialization.json.JsonObject.entryIds() =
    getValue("data").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content }

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? = content.takeIf { it != "null" }
