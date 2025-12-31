package com.example

import com.example.configuration.*
import com.example.data.database.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureDependencies() // installs Koin
    configureCORS()
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureAuthentication()
    configureLogging()
    configureRouting() // The routes itself
}