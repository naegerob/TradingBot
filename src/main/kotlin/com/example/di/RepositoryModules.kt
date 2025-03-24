package com.example.di

import com.example.data.TradingRepository
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.engine.cio.*

val appModule = module {
    single { HttpClient(CIO) } // Real HTTP client
    single<TradingRepository> { AlpacaRepository(get()) } // Real API implementation
}

val testModule = module {
    single { HttpClient()}
    single<TradingRepository> { MockTradingRepository() } // Inject mock repo
}