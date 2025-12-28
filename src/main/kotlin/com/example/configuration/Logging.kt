package com.example.configuration

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.DEBUG
        // optional: filtere z.B. Health-Checks
        // filter { call -> call.request.path() != "/health" }
    }
}
