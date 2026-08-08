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

private const val SEED_FILE = "seed.json"

@Serializable
private data class SeedData(
    val accounts: List<OpenAccountDto>,
    val entries: List<RecordAccountEntryDto>,
)

fun Application.configureSeeding(port: Int) {
    val ledger = "http://localhost:$port"

    monitor.subscribe(ServerReady) {
        launch {
            val seed = Json.decodeFromString<SeedData>(readSeedFile())

            HttpClient(CIO).use { client ->
                seed.accounts.forEach { client.send("$ledger/accounts", it) }
                seed.entries.forEach { client.send("$ledger/journal/entries", it) }
            }

            log.info("Seeded ${seed.accounts.size} accounts and ${seed.entries.size} entries from $SEED_FILE")
        }
    }
}

private fun readSeedFile(): String {
    val stream = object {}.javaClass.classLoader.getResourceAsStream(SEED_FILE)
        ?: error("No seed file named \"$SEED_FILE\" on the classpath")

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
