package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
    install(DefaultHeaders) {
        header("content-type", "application/json")
        header("accept", "application/json")
    }
}
