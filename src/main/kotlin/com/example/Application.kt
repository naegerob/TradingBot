package com.example

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization() // For Json/XML and so on
    configureDatabases()
    configureMonitoring()
    configureRouting()      // For HTTP requests
}