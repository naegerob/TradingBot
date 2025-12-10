package com.example.application

import com.example.data.AlpacaRepository
import com.example.data.AlpacaRepository.Companion.PAPERAPIKEY
import com.example.data.AlpacaRepository.Companion.PAPERSECRET
import com.example.data.TradingRepository
import com.example.tradingLogic.TradingBot
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val appModule = module {
    single<HttpClientEngine> { CIO.create() }
    single<TradingRepository> { AlpacaRepository() }
    single<TradingBot> { TradingBot() }
    single<HttpClient> { // Alpaca API
        HttpClient(get()) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = false
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
            install(DefaultRequest) {
                header("APCA-API-KEY-ID", PAPERAPIKEY)
                header("APCA-API-SECRET-KEY", PAPERSECRET)
                header("content-type", "application/json")
                header("accept", "application/json")
            }
        }
    }
}

fun Application.configureDependencies() {
    install(Koin) {
        modules(appModule)
    }
}

