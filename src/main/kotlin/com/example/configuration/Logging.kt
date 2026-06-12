package com.example.configuration

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.http.*
import org.slf4j.event.Level

fun Application.configureLogging() {
    install(CallLogging) {
        // One concise access log per completed API call.
        level = Level.INFO
    }
}

