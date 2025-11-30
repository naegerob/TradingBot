package com.example

import io.ktor.http.*
import io.ktor.server.application.*


fun Application.configureCORS() {
    install(io.ktor.server.plugins.cors.routing.CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true
        allowHost("localhost:5173", schemes = listOf("http", "https"))
    }
}