package altak.ledger

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Where a test swaps a dependency out. The module is listed after `rootModule`, and
 * `ktor.di.conflictPolicy = OverridePrevious` lets what it declares win, so anything the server binds
 * to the outside world — a database, a cache, a payment gateway — is replaced here by whatever a test
 * should talk to instead, without the production wiring knowing that tests exist.
 *
 * Nothing here reaches outside the process yet; the clock and the zone are pinned so that dates and
 * the version 7 ids derived from them do not depend on when or where the suite runs.
 */
fun Application.testDependencies() {
    dependencies {
        provide<Clock> { advancingClock() }
        provide<TimeZone> { ZONE }
    }
}
