package com.example

import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = false
            encodeDefaults = true
            explicitNulls = false
        })
    }
    install(DefaultHeaders) {
        header("content-type", "application/json")
        header("accept", "application/json")
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if(cause is SerializationException) {
                call.respondText(text = "Could not serialize: $cause" , status = HttpStatusCode.InternalServerError)
            } else if(cause is ContentConvertException) {
                call.respondText(text = "Could not convert: $cause" , status = HttpStatusCode.InternalServerError)
            } else {
                call.respondText(text = "Everything else which I had no idea about: $cause" , status = HttpStatusCode.InternalServerError)
            }
        }
    }
}
