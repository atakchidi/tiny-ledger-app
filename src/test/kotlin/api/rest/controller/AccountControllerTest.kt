package altak.ledger.api.rest.controller

import altak.ledger.api.rest.apiTest
import altak.ledger.api.rest.data
import altak.ledger.api.rest.errors
import altak.ledger.api.rest.field
import altak.ledger.api.rest.id
import altak.ledger.api.rest.nextCursor
import altak.ledger.api.rest.records
import altak.ledger.domain.account.AccountReference
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccountControllerTest {

    private fun JsonObject.text(name: String) = getValue(name).jsonPrimitive.content

    private suspend fun HttpClient.openAccount(
        name: String = "Alice",
        currency: String = "EUR",
        reference: String = "ACC-ALICE",
    ) = post("/accounts") {
        contentType(ContentType.Application.Json)
        setBody(
            """
            {
              "name": "$name",
              "currency": "$currency",
              "reference": "$reference"
            }
            """
        )
    }

    @Test
    fun `opens an account`() = apiTest {
        val response = client.openAccount()

        assertEquals(HttpStatusCode.Created, response.status)
        with(response.data()) {
            assertEquals("Alice", text("name"))
            assertEquals("ACC-ALICE", text("reference"))
            assertEquals("EUR", text("currency"))
            assertEquals("LIABILITY", text("type"))
            assertEquals("0.00", text("balance"))
            assertNotNull(text("id"))
            assertEquals(text("createdAt"), text("updatedAt"))
        }
    }

    @Test
    fun `views an account by id and by the reference it is known by outside`() = apiTest {
        val id = client.openAccount().id()

        val byId = client.get("/accounts/$id")
        val byReference = client.get("/accounts/ACC-ALICE")

        assertEquals(HttpStatusCode.OK, byId.status)
        assertEquals(id, byId.field("id"))
        assertEquals(HttpStatusCode.OK, byReference.status)
        assertEquals(id, byReference.field("id"))
    }

    @Test
    fun `lists the accounts on the books`() = apiTest {
        client.openAccount()
        client.openAccount(name = "Bob", reference = "ACC-BOB")

        val all = client.get("/accounts")

        assertEquals(HttpStatusCode.OK, all.status)
        assertEquals(
            setOf("ACC-ALICE", "ACC-BOB"),
            all.records().map { it.jsonObject.text("reference") }.toSet(),
        )
        assertNull(all.nextCursor())
    }

    @Test
    fun `answers the listing a page at a time`() = apiTest {
        client.openAccount()
        client.openAccount(name = "Bob", reference = "ACC-BOB")

        val firstPage = client.get("/accounts?limit=1")

        assertEquals(1, firstPage.records().size)
        assertEquals(firstPage.records().single().jsonObject.text("id"), firstPage.nextCursor())
    }

    @Test
    fun `refuses a request that breaks a validation rule`() = apiTest {
        val blankName = client.openAccount(name = "")
        val notACurrency = client.openAccount(currency = "XYZ")

        assertEquals(HttpStatusCode.BadRequest, blankName.status)
        assertEquals(listOf("name: must not be blank"), blankName.errors())
        assertEquals(HttpStatusCode.BadRequest, notACurrency.status)
        assertEquals(listOf("\"XYZ\" is not an ISO 4217 currency code"), notACurrency.errors())
    }

    @Test
    fun `refuses a reference that is not upper-case letters, digits or dashes`() = apiTest {
        val lowercase = client.openAccount(reference = "acc-alice")
        val tooShort = client.openAccount(reference = "AB")
        val leadingDash = client.openAccount(reference = "-LEADING")

        val mustMatch = """reference: must match "${AccountReference.FORMAT}""""
        assertEquals(HttpStatusCode.BadRequest, lowercase.status)
        assertEquals(listOf(mustMatch), lowercase.errors())
        assertEquals(HttpStatusCode.BadRequest, tooShort.status)
        assertEquals(listOf("reference: length must be between 3 and 32", mustMatch), tooShort.errors())
        assertEquals(HttpStatusCode.BadRequest, leadingDash.status)
        assertEquals(listOf(mustMatch), leadingDash.errors())
    }

    @Test
    fun `refuses to order the accounts by a field it does not offer`() = apiTest {
        val response = client.get("/accounts?sort=name")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(listOf("sort: must be one of id, reference"), response.errors())
    }

    @Test
    fun `answers a conflict when the reference is already open`() = apiTest {
        client.openAccount()

        val second = client.openAccount(name = "Bob")

        assertEquals(HttpStatusCode.Conflict, second.status)
        assertEquals(listOf("Account by reference 'ACC-ALICE' is already open."), second.errors())
    }

    @Test
    fun `answers not found for an account it does not keep`() = apiTest {
        val unknown = client.get("/accounts/019fffff-0000-7000-8000-000000000000")
        val malformed = client.get("/accounts/not-an-account")

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals(
            listOf("Account by id '019fffff-0000-7000-8000-000000000000' not found."),
            unknown.errors(),
        )
        assertEquals(HttpStatusCode.NotFound, malformed.status)
        assertEquals(listOf("Account by id 'not-an-account' not found."), malformed.errors())
    }
}
