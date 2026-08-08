package altak.ledger.infrastructure.ktor

import altak.ledger.infrastructure.ktor.plugins.configureAutoHeadResponse
import altak.ledger.infrastructure.ktor.plugins.configureDependencyInjection
import altak.ledger.infrastructure.ktor.plugins.configureHttp
import altak.ledger.infrastructure.ktor.plugins.configureRouting
import altak.ledger.infrastructure.ktor.plugins.configureSeeding
import altak.ledger.infrastructure.ktor.plugins.configureSerialization
import altak.ledger.infrastructure.ktor.plugins.configureStatusPages
import altak.ledger.infrastructure.ktor.plugins.configureValidation
import io.ktor.server.application.Application

fun Application.rootModule() {
    configureDependencyInjection()
    configureAutoHeadResponse()
    configureHttp()
    configureSerialization()
    configureStatusPages()
    configureValidation()
    configureRouting()
    configureSeeding()
}
