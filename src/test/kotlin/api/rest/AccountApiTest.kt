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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountApiTest {

    private fun ApplicationTestBuilder.startServer() = application { rootModule() }

    private suspend fun HttpClient.openAccount(
        name: String = "Alice",
        currency: String = "EUR",
        reference: String = "ACC-ALICE",
    ) = post("/accounts") {
        contentType(ContentType.Application.Json)
        setBody("""{"name":"$name","currency":"$currency","reference":"$reference"}""")
    }

    @Test
    fun `opens an account`() = testApplication {
        startServer()

        val response = client.openAccount()

        assertEquals(HttpStatusCode.Created, response.status)
        with(response.bodyAsText()) {
            assertContains(this, """"name":"Alice"""")
            assertContains(this, """"reference":"ACC-ALICE"""")
            assertContains(this, """"currency":"EUR"""")
            assertContains(this, """"type":"LIABILITY"""")
            assertContains(this, """"balance":"0.00"""")
        }
    }

    @Test
    fun `orders the accounts by the reference they are known by`() = testApplication {
        startServer()
        client.openAccount(name = "Chloe", reference = "ACC-003")
        client.openAccount(name = "Alice", reference = "ACC-001")

        val byReference = client.get("/accounts?sort=reference")

        assertEquals(
            listOf("ACC-001", "ACC-003"),
            byReference.records().map { it.jsonObject.getValue("reference").jsonPrimitive.content },
        )
    }

    @Test
    fun `refuses to order the accounts by a field it does not offer`() = testApplication {
        startServer()

        val response = client.get("/accounts?sort=name")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "sort: must be one of id, reference")
    }

    @Test
    fun `rejects a request that breaks a validation rule`() = testApplication {
        startServer()

        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"","currency":"EUR","reference":"ACC-ALICE"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "name")
    }

    @Test
    fun `rejects a reference that is not in canonical form`() = testApplication {
        startServer()

        val response = client.openAccount(reference = "acc-alice")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "reference")
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
    fun `refuses to open a second account under the same reference`() = testApplication {
        startServer()

        client.openAccount()
        val second = client.openAccount(name = "Bob")

        assertEquals(HttpStatusCode.Conflict, second.status)
        assertContains(second.bodyAsText(), "is already open")
    }

    @Test
    fun `views an account by id and by reference`() = testApplication {
        startServer()
        val id = client.openAccount().id()

        val byId = client.get("/accounts/$id")
        val byReference = client.get("/accounts/ACC-ALICE")

        assertEquals(HttpStatusCode.OK, byId.status)
        assertEquals(id, byId.field("id"))
        assertEquals(HttpStatusCode.OK, byReference.status)
        assertEquals(id, byReference.field("id"))
    }

    @Test
    fun `lists the accounts on the books, a page at a time`() = testApplication {
        startServer()
        client.openAccount()
        client.openAccount(name = "Bob", reference = "ACC-BOB")

        val all = client.get("/accounts")
        val firstPage = client.get("/accounts?limit=1")

        assertEquals(
            setOf("ACC-ALICE", "ACC-BOB"),
            all.records().map { it.jsonObject.getValue("reference").jsonPrimitive.content }.toSet(),
        )
        assertNull(all.nextCursor())
        assertEquals(1, firstPage.records().size)
        assertEquals(firstPage.records().single().jsonObject.getValue("id").jsonPrimitive.content, firstPage.nextCursor())
    }

    @Test
    fun `has nothing to show for an account it does not keep`() = testApplication {
        startServer()

        val unknown = client.get("/accounts/019fffff-0000-7000-8000-000000000000")
        val malformed = client.get("/accounts/not-an-account")

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals(HttpStatusCode.NotFound, malformed.status)
        assertContains(malformed.bodyAsText(), "not found")
    }
}
