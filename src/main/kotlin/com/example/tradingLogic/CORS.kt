package com.example.tradingLogic

import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.server.application.*


fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        allowHost("localhost:5173") // your frontend origin
    }
}