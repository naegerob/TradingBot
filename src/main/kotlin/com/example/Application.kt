package com.example

import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.core.context.startKoin

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(){
    configureDependencies()
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureDatabases()
    configureMonitoring()
    configureRouting() // The routes itself
}