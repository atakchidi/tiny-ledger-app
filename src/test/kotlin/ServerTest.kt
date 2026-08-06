package altak

import altak.infrastructure.ktor.rootModule
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    private fun ApplicationTestBuilder.startServer() = application { rootModule() }

    @Test
    fun `test greeting endpoint`() = testApplication {
        startServer()
        val response = client.get("/greeting")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, World!", response.bodyAsText())
    }

    @Test
    fun `test json endpoint`() = testApplication {
        startServer()
        val response = client.get("/json/kotlinx-serialization")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"hello":"world"}""", response.bodyAsText())
    }

    @Test
    fun `open account accepts a valid request`() = testApplication {
        startServer()
        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Savings","currency":"EUR","openingBalance":1000}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertContains(body, """"name":"Savings"""")
        assertContains(body, """"currency":"EUR"""")
        assertContains(body, """"balance":1000""")
    }

    @Test
    fun `open account rejects a request violating every constraint`() = testApplication {
        startServer()
        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"","currency":"eur","openingBalance":-5}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            """{"errors":["currency: must be a 3-letter ISO 4217 code",""" +
                """"name: must not be blank","openingBalance: must not be negative"]}""",
            response.bodyAsText(),
        )
    }

    @Test
    fun `open account rejects a name over the length limit`() = testApplication {
        startServer()
        val response = client.post("/accounts") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"${"a".repeat(65)}","currency":"EUR","openingBalance":0}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            """{"errors":["name: must be at most 64 characters"]}""",
            response.bodyAsText(),
        )
    }
}
