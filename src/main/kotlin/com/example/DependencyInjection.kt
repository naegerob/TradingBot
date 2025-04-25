package com.example

import io.ktor.client.engine.cio.*
import io.ktor.server.application.*
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val appModule = module {
    single { CIO.create() }
}

fun Application.configureDependencies() {
    install(Koin) {
        modules(appModule)
    }
}

