package com.example

import io.ktor.http.*
import io.ktor.server.application.*


fun Application.configureCORS() {
    install(io.ktor.server.plugins.cors.routing.CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        allowHost("localhost:5173") // your frontend origin
    }
}