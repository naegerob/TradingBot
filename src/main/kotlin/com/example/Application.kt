package com.example

import com.example.di.configureDependencies
import io.ktor.server.application.*
import io.ktor.server.netty.*

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