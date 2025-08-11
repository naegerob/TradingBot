package com.example

import com.example.di.configureDependencies
import configureAuthentication
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(){
    configureDependencies() // installs Koin
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureAuthentication()
    configureMonitoring()
    configureRouting() // The routes itself
}