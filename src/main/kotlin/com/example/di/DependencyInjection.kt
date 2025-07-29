package com.example.di

import com.example.data.AlpacaRepository
import com.example.data.TradingRepository
import com.example.tradingLogic.TradingBot
import com.example.tradingLogic.TradingController
import io.ktor.client.engine.cio.*
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val appModule = module {
    single { CIO.create() }
    single<TradingRepository> { AlpacaRepository() }
    single { TradingBot() }
    single { TradingController() }
}

fun Application.configureDependencies() {
    install(Koin) {
        modules(appModule)
    }
}

