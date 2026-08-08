package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.journal.RecordAccountEntryDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.application.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SeedData(
    val accounts: List<OpenAccountDto>,
    val entries: List<RecordAccountEntryDto>,
)

// A running server fills itself through its own API. HOCON merges every application.conf on the
// classpath, so a test cannot start empty by leaving `ledger.seed` out — it names it blank instead.
fun Application.configureSeeding() {
    val seedFile = environment.config.propertyOrNull("ledger.seed")?.getString()?.ifBlank { null } ?: return
    val ledger = "http://localhost:${environment.config.property("ktor.deployment.port").getString()}"

    monitor.subscribe(ServerReady) {
        launch {
            val seed = Json.decodeFromString<SeedData>(readSeedFile(seedFile))

            HttpClient(CIO).use { client ->
                seed.accounts.forEach { client.send("$ledger/accounts", it) }
                seed.entries.forEach { client.send("$ledger/journal/entries", it) }
            }

            log.info("Seeded ${seed.accounts.size} accounts and ${seed.entries.size} entries from $seedFile")
        }
    }
}

private fun readSeedFile(name: String): String {
    val stream = object {}.javaClass.classLoader.getResourceAsStream(name)
        ?: error("No seed file named \"$name\" on the classpath")

    return stream.use { it.readBytes().decodeToString() }
}

private suspend inline fun <reified T> HttpClient.send(url: String, body: T) {
    val response = post(url) {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(body))
    }

    if (!response.status.isSuccess()) {
        error("Seeding $url answered ${response.status}: ${response.bodyAsText()}")
    }
}
