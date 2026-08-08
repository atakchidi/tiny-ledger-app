package altak.ledger.api.rest

import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

/**
 * `testApplication` starts with an empty config rather than the one on the classpath, so the file is
 * named here. Loading it — instead of installing the module by hand — keeps a test on the same wiring
 * the server boots with: `src/test/resources/application.conf` shadows the deployed one, adds
 * `testDependencies` after `rootModule`, and declares no seed, so a test starts on empty books.
 */
fun apiTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    configure("application.conf")

    block()
}
