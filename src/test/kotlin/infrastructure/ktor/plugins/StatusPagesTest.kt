package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.apiTest
import altak.ledger.api.rest.errors
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusPagesTest {

    @Test
    fun `refuses a body that is not an object`() = apiTest {
        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("not json at all")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            listOf("Unexpected JSON token at offset 0: Expected start of the object '{', but had 'n' instead at path: $"),
            response.errors(),
        )
    }

    @Test
    fun `names the field a body left out`() = apiTest {
        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alice","currency":"EUR"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            listOf(
                "Field 'reference' is required for type with serial name " +
                    "'altak.ledger.application.account.OpenAccountDto', but it was missing at path: $",
            ),
            response.errors(),
        )
    }

    @Test
    fun `names the field a body gave the wrong kind of value`() = apiTest {
        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":[],"currency":"EUR","reference":"ACC-ALICE"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            listOf("Unexpected JSON token at offset 8: Expected beginning of the string, but got [ at path: \$.name"),
            response.errors(),
        )
    }
}
