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
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class AccountApiTest {

    private fun ApplicationTestBuilder.startServer() = application { rootModule() }

    private suspend fun HttpClient.openAccount(name: String = "Alice", currency: String = "EUR") =
        post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","currency":"$currency"}""")
        }

    @Test
    fun `opens an account`() = testApplication {
        startServer()

        val response = client.openAccount()

        assertEquals(HttpStatusCode.Created, response.status)
        with(response.bodyAsText()) {
            assertContains(this, """"name":"Alice"""")
            assertContains(this, """"currency":"EUR"""")
            assertContains(this, """"type":"LIABILITY"""")
            assertContains(this, """"balance":0.00""")
        }
    }

    @Test
    fun `rejects a request that breaks a validation rule`() = testApplication {
        startServer()

        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"","currency":"EUR","reference":"no"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            """{"errors":["name: must not be blank","reference: must be 3 to 32 letters, digits or dashes"]}""",
            response.bodyAsText(),
        )
    }

    @Test
    fun `rejects a currency that is not in the ISO register`() = testApplication {
        startServer()

        val unknown = client.openAccount(currency = "XYZ")
        val lowercase = client.openAccount(currency = "eur")

        assertEquals(HttpStatusCode.BadRequest, unknown.status)
        assertContains(unknown.bodyAsText(), """\"XYZ\" is not an ISO 4217 currency code""")
        assertEquals(HttpStatusCode.BadRequest, lowercase.status)
        assertContains(lowercase.bodyAsText(), """\"eur\" is not an ISO 4217 currency code""")
    }

    @Test
    fun `views an account and its balance`() = testApplication {
        startServer()
        val id = client.openAccount().id()

        val account = client.get("/accounts/$id")
        val balance = client.get("/accounts/$id/balance")

        assertEquals(HttpStatusCode.OK, account.status)
        assertContains(account.bodyAsText(), """"id":"$id"""")
        assertEquals(HttpStatusCode.OK, balance.status)
        assertContains(balance.bodyAsText(), """"amount":0.00""")
        assertContains(balance.bodyAsText(), """"currency":"EUR"""")
    }

    @Test
    fun `lists the accounts on the books`() = testApplication {
        startServer()
        client.openAccount()
        client.openAccount(name = "Bob")

        val response = client.get("/accounts")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), """"name":"Alice"""")
        assertContains(response.bodyAsText(), """"name":"Bob"""")
    }

    @Test
    fun `is reachable by the reference it is known by outside`() = testApplication {
        startServer()
        client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","currency":"EUR","reference":"acc-000123"}""")
        }

        val balance = client.get("/accounts/ACC-000123/balance")

        assertEquals(HttpStatusCode.OK, balance.status)
        assertContains(balance.bodyAsText(), """"amount":0.00""")
    }

    @Test
    fun `refuses to open a second account under the same reference`() = testApplication {
        startServer()
        val body = """{"name":"Alice","currency":"EUR","reference":"acc-000123"}"""

        client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val second = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        assertEquals(HttpStatusCode.Conflict, second.status)
        assertContains(second.bodyAsText(), "is already open")
    }

    @Test
    fun `has nothing to show for an account it does not keep`() = testApplication {
        startServer()

        val unknown = client.get("/accounts/0199ffff-0000-7000-8000-000000000000")
        val malformed = client.get("/accounts/not-an-account/balance")

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals(HttpStatusCode.NotFound, malformed.status)
        assertContains(malformed.bodyAsText(), "not found")
    }
}
