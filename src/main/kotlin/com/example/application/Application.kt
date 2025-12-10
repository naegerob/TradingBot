package com.example.application

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(){
    configureDependencies() // installs Koin
    configureCORS()
    configureLogging()
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureAuthentication()
    configureMonitoring()
    configureRouting() // The routes itself
}