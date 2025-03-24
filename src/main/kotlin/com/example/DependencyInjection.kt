package com.example

import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin


fun Application.configureDependencies() {
    // Install Koin
    install(Koin) {
        modules()
    }
}
