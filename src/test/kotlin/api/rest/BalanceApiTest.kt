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

class BalanceApiTest {

    private fun ApplicationTestBuilder.startServer() = application { rootModule() }

    private suspend fun HttpClient.openAccount(reference: String) =
        post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","currency":"EUR","reference":"$reference"}""")
        }

    private suspend fun HttpClient.deposit(account: String, amount: String) =
        post("/accounts/$account/entries") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"DEPOSIT","amount":$amount}""")
        }

    private suspend fun HttpClient.balances(query: String = "") =
        Json.parseToJsonElement(get("/balances$query").bodyAsText()).jsonObject.getValue("data").jsonArray

    private fun kotlinx.serialization.json.JsonArray.amountOf(reference: String) =
        map { it.jsonObject }
            .single { it.getValue("reference").jsonPrimitive.content == reference }
            .getValue("amount")
            .jsonPrimitive
            .content

    @Test
    fun `answers for every account when no account is named`() = testApplication {
        startServer()
        client.openAccount("ACC-ALICE")
        client.openAccount("ACC-BOB")
        client.deposit("ACC-ALICE", "10.00")
        client.deposit("ACC-BOB", "2.50")

        val balances = client.balances()

        assertEquals(
            setOf("ACC-ALICE", "ACC-BOB", "CASH-EUR"),
            balances.map { it.jsonObject.getValue("reference").jsonPrimitive.content }.toSet(),
        )
        assertEquals("10.00", balances.amountOf("ACC-ALICE"))
        assertEquals("2.50", balances.amountOf("ACC-BOB"))
        assertEquals("12.50", balances.amountOf("CASH-EUR"))
    }

    @Test
    fun `answers for one account when it is named, by id or by reference`() = testApplication {
        startServer()
        val id = client.openAccount("ACC-ALICE").id()
        client.deposit("ACC-ALICE", "10.00")

        val byReference = client.balances("?account=ACC-ALICE")
        val byId = client.balances("?account=$id")

        assertEquals(1, byReference.size)
        assertEquals("10.00", byReference.amountOf("ACC-ALICE"))
        assertEquals("10.00", byId.amountOf("ACC-ALICE"))
    }

    @Test
    fun `reads the journal as it stood on the date it is given`() = testApplication {
        startServer()
        client.openAccount("ACC-ALICE")
        client.deposit("ACC-ALICE", "10.00")

        val before = client.balances("?account=ACC-ALICE&onDate=2020-01-01T00:00:00Z")
        val after = client.balances("?account=ACC-ALICE&onDate=2099-01-01T00:00:00Z")

        assertEquals("0.00", before.amountOf("ACC-ALICE"))
        assertEquals("10.00", after.amountOf("ACC-ALICE"))
    }

    @Test
    fun `says which moment it answered for`() = testApplication {
        startServer()
        client.openAccount("ACC-ALICE")

        val balances = client.balances("?onDate=2026-08-01T09:30:00Z")

        assertEquals(
            "2026-08-01T09:30:00Z",
            balances.single().jsonObject.getValue("onDate").jsonPrimitive.content,
        )
    }

    @Test
    fun `refuses a date it cannot read`() = testApplication {
        startServer()

        val response = client.get("/balances?onDate=yesterday")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "is not an ISO-8601 moment")
    }

    @Test
    fun `has nothing to show for an account it does not keep`() = testApplication {
        startServer()

        val response = client.get("/balances?account=ACC-NOBODY")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "not found")
    }
}
