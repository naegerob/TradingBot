package com.example

import com.example.data.AlpacaRepository
import com.example.data.AlpacaRepository.Companion.PAPERAPIKEY
import com.example.data.AlpacaRepository.Companion.PAPERSECRET
import com.example.data.TradingRepository
import com.example.tradingLogic.TradingController
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(alpacaClient: HttpClient = HttpClient(CIO) {


    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    engine {
        requestTimeout = 0 // 0 to disable, or a millisecond value to fit your needs
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
    }
    install(DefaultRequest) {
        header("APCA-API-KEY-ID", PAPERAPIKEY)
        header("APCA-API-SECRET-KEY", PAPERSECRET)
        header("content-type", "application/json")
        header("accept", "application/json")
    }
}){
    val service by inject<TradingRepository>()
    configureDependencies()
    configureSerialization() // Configures the contentNegotiation (XML,JSON,...)
    configureDatabases()
    configureMonitoring()
    configureRouting(TradingController(alpacaClient)) // The routes itself
}