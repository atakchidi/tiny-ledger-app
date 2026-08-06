package altak.infrastructure.ktor

import altak.infrastructure.ktor.plugins.configureAutoHeadResponse
import altak.infrastructure.ktor.plugins.configureDependencyInjection
import altak.infrastructure.ktor.plugins.configureHttp
import altak.infrastructure.ktor.plugins.configureRouting
import altak.infrastructure.ktor.plugins.configureSerialization
import altak.infrastructure.ktor.plugins.configureStatusPages
import altak.infrastructure.ktor.plugins.configureValidation
import io.ktor.server.application.Application

fun Application.rootModule() {
    configureDependencyInjection()
    configureAutoHeadResponse()
    configureHttp()
    configureSerialization()
    configureStatusPages()
    configureValidation()
    configureRouting()
}
