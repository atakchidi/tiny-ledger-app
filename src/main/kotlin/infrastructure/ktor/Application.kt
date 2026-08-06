package altak.Infrastructure.Ktor

import altak.Infrastructure.Ktor.plugins.configureAutoHeadResponse
import altak.Infrastructure.Ktor.plugins.configureDependencyInjection
import altak.Infrastructure.Ktor.plugins.configureHttp
import altak.Infrastructure.Ktor.plugins.configureRouting
import altak.Infrastructure.Ktor.plugins.configureSerialization
import io.ktor.server.application.Application

fun Application.rootModule() {
    configureDependencyInjection()
    configureAutoHeadResponse()
    configureHttp()
    configureSerialization()
    configureRouting()
}
