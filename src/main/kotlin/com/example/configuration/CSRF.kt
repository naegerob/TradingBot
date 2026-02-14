package com.example.configuration

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.csrf.CSRF

fun Application.configureCSRF() {
    install(CSRF) {
        allowOrigin("https://localhost:8081")
        allowOrigin("https://localhost:5173")
        allowOrigin("http://localhost:8081")
        allowOrigin("http://localhost:5173")
        checkHeader("X-CSRF-Token")
    }
}