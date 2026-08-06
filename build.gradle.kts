import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "altak"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "altak.MainKt"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.autoHeadResponse)
    implementation(ktorLibs.server.compression)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.openapi)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.routingOpenapi)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.logback.classic)
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)
    runtimeOnly(libs.expressly)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
