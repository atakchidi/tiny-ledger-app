package altak.ledger.api.rest.controller

import altak.ledger.api.rest.apiTest
import altak.ledger.api.rest.data
import altak.ledger.api.rest.errors
import altak.ledger.api.rest.field
import altak.ledger.api.rest.id
import altak.ledger.api.rest.nextCursor
import altak.ledger.api.rest.records
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JournalControllerTest {

    private fun JsonObject.text(name: String) = getValue(name).jsonPrimitive.content

    private fun JsonObject.lineOf(reference: String) =
        getValue("lines").jsonArray.map { it.jsonObject }.single { it.text("reference") == reference }

    private suspend fun HttpClient.openAccount(reference: String = "ACC-ALICE") =
        post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "Alice",
                  "currency": "EUR",
                  "reference": "$reference"
                }
                """
            )
        }

    private suspend fun HttpClient.record(body: String) =
        post("/journal/entries") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpClient.deposit(account: String = "ACC-ALICE", amount: String = "10.00") =
        record(
            """
            {
              "account": "$account",
              "type": "DEPOSIT",
              "amount": $amount
            }
            """
        )

    @Test
    fun `records a movement, and answers with both sides of the entry it was posted as`() = apiTest {
        client.openAccount()

        val response = client.record(
            """
            {
              "account": "ACC-ALICE",
              "type": "DEPOSIT",
              "amount": 10.50,
              "description": "Salary"
            }
            """
        )

        assertEquals(HttpStatusCode.Created, response.status)
        with(response.data()) {
            assertEquals("Salary", text("description"))
            assertEquals("EUR", text("currency"))
            assertEquals("10.50", text("totalDebit"))
            assertEquals("10.50", text("totalCredit"))
            assertNotNull(text("id"))
            assertNotNull(text("occurredOn"))
            assertNotNull(text("createdAt"))
            with(lineOf("ACC-ALICE")) {
                assertEquals("LIABILITY", text("accountType"))
                assertEquals("CREDIT", text("direction"))
                assertEquals("10.50", text("amount"))
                assertNotNull(text("accountId"))
            }
            with(lineOf("CASH-EUR")) {
                assertEquals("ASSET", text("accountType"))
                assertEquals("DEBIT", text("direction"))
                assertEquals("10.50", text("amount"))
            }
        }
    }

    @Test
    fun `takes an entry dated back to the day it happened`() = apiTest {
        client.openAccount()

        val entry = client.record(
            """
            {
              "account": "ACC-ALICE",
              "type": "DEPOSIT",
              "amount": 10.00,
              "occurredOn": "2026-01-31"
            }
            """
        )

        assertEquals(HttpStatusCode.Created, entry.status)
        assertEquals("2026-01-31", entry.field("occurredOn"))
    }

    @Test
    fun `lists the whole journal, and one account's part of it`() = apiTest {
        client.openAccount()
        client.openAccount(reference = "ACC-BOB")
        val hers = client.deposit().id()
        val his = client.deposit(account = "ACC-BOB").id()

        val whole = client.get("/journal/entries")
        val alices = client.get("/journal/entries?account=ACC-ALICE")

        assertEquals(HttpStatusCode.OK, whole.status)
        assertEquals(setOf(hers, his), whole.records().map { it.jsonObject.text("id") }.toSet())
        assertEquals(HttpStatusCode.OK, alices.status)
        assertEquals(listOf(hers), alices.records().map { it.jsonObject.text("id") })
    }

    @Test
    fun `answers the journal a page at a time`() = apiTest {
        client.openAccount()
        (1..3).forEach { client.deposit(amount = "$it.00") }

        val firstPage = client.get("/journal/entries?limit=2")

        assertEquals(2, firstPage.records().size)
        assertEquals(firstPage.records().last().jsonObject.text("id"), firstPage.nextCursor())
    }

    @Test
    fun `lists the balances, read as of the date the query names`() = apiTest {
        client.openAccount()
        client.deposit(amount = "10.50")

        val today = client.get("/journal/balances?account=ACC-ALICE")
        val before = client.get("/journal/balances?account=ACC-ALICE&onDate=2026-06-01")

        assertEquals(HttpStatusCode.OK, today.status)
        with(today.records().single().jsonObject) {
            assertEquals("ACC-ALICE", text("reference"))
            assertEquals("EUR", text("currency"))
            assertEquals("10.50", text("amount"))
            assertNotNull(text("accountId"))
            assertNotNull(text("onDate"))
        }
        with(before.records().single().jsonObject) {
            assertEquals("2026-06-01", text("onDate"))
            assertEquals("0.00", text("amount"))
        }
    }

    @Test
    fun `refuses an entry dated after today`() = apiTest {
        client.openAccount()

        val entry = client.record(
            """
            {
              "account": "ACC-ALICE",
              "type": "DEPOSIT",
              "amount": 10.00,
              "occurredOn": "2099-12-31"
            }
            """
        )

        assertEquals(HttpStatusCode.BadRequest, entry.status)
        assertEquals(listOf("occurredOn: must be a date in the past or in the present"), entry.errors())
    }

    @Test
    fun `refuses an amount it cannot read or would not post`() = apiTest {
        client.openAccount()

        val tooPrecise = client.deposit(amount = "10.505")
        val notANumber = client.deposit(amount = "\"ten\"")
        val nothing = client.deposit(amount = "0.00")

        assertEquals(HttpStatusCode.BadRequest, tooPrecise.status)
        assertEquals(listOf("10.505 is finer than EUR can hold"), tooPrecise.errors())
        assertEquals(HttpStatusCode.BadRequest, notANumber.status)
        assertEquals(listOf("\"ten\" is not a decimal number"), notANumber.errors())
        assertEquals(HttpStatusCode.BadRequest, nothing.status)
        assertEquals(listOf("amount: must be a positive amount"), nothing.errors())
    }

    @Test
    fun `names the movements it knows when it is given one it does not`() = apiTest {
        client.openAccount()

        val unknown = client.record(
            """
            {
              "account": "ACC-ALICE",
              "type": "TRANSFER",
              "amount": 10.00
            }
            """
        )

        assertEquals(HttpStatusCode.BadRequest, unknown.status)
        assertEquals(listOf("\"TRANSFER\" is not one of DEPOSIT, WITHDRAWAL"), unknown.errors())
    }

    @Test
    fun `refuses to order the entries by a field it does not offer`() = apiTest {
        val response = client.get("/journal/entries?sort=description")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(listOf("sort: must be one of id, occurredOn"), response.errors())
    }

    @Test
    fun `refuses a page nobody could fill`() = apiTest {
        val empty = client.get("/journal/entries?limit=0")
        val beyondTheLedger = client.get("/journal/entries?limit=500")

        assertEquals(HttpStatusCode.BadRequest, empty.status)
        assertEquals(listOf("limit: must be greater than or equal to 1"), empty.errors())
        assertEquals(HttpStatusCode.BadRequest, beyondTheLedger.status)
        assertEquals(listOf("limit: must be less than or equal to 200"), beyondTheLedger.errors())
    }

    @Test
    fun `refuses a cursor and a date it cannot read`() = apiTest {
        val cursor = client.get("/journal/entries?after=not-an-entry")
        val date = client.get("/journal/balances?onDate=yesterday")

        assertEquals(HttpStatusCode.BadRequest, cursor.status)
        assertEquals(listOf("\"not-an-entry\" is not an identifier"), cursor.errors())
        assertEquals(HttpStatusCode.BadRequest, date.status)
        assertEquals(listOf("\"yesterday\" is not a date, as YYYY-MM-DD"), date.errors())
    }

    @Test
    fun `answers not found on an account it does not keep`() = apiTest {
        val recorded = client.deposit(account = "ACC-NOBODY")
        val listed = client.get("/journal/entries?account=ACC-NOBODY")
        val balanced = client.get("/journal/balances?account=ACC-NOBODY")

        val notFound = listOf("Account by id 'ACC-NOBODY' not found.")
        assertEquals(HttpStatusCode.NotFound, recorded.status)
        assertEquals(notFound, recorded.errors())
        assertEquals(HttpStatusCode.NotFound, listed.status)
        assertEquals(notFound, listed.errors())
        assertEquals(HttpStatusCode.NotFound, balanced.status)
        assertEquals(notFound, balanced.errors())
    }
}
