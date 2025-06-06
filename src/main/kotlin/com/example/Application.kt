package com.example

import com.example.tradingLogic.TradingController
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureDatabases()
    configureMonitoring()
    configureRouting(TradingController()) // The routes itself
}