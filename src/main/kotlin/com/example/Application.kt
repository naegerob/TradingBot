package com.example

import com.example.di.configureDependencies
import com.example.tradingLogic.configureCORS
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(){
    configureCORS()
    configureDependencies() // installs Koin
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureMonitoring()
    configureRouting() // The routes itself
}