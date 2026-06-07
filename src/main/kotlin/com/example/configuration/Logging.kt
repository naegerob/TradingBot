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

        filter { call ->
            val path = call.request.path()
            path != "/health" && path != "/ready" && path != "/metrics" && call.request.httpMethod != HttpMethod.Options
        }

        mdc("requestId") { call ->
            call.request.headers["X-Request-ID"] ?: call.request.headers["X-Correlation-ID"]
        }

        format { call ->
            val request = call.request
            val response = call.response
            val method = request.httpMethod.value
            val path = request.path()
            val query = request.queryString().ifBlank { "-" }
            val status = response.status()?.value ?: 0
            val durationMs = call.processingTimeMillis()
            val clientIp = request.local.remoteHost
            val reqContentType = request.headers[HttpHeaders.ContentType] ?: "-"
            val resContentType = response.headers[HttpHeaders.ContentType] ?: "-"
            val requestId = request.headers["X-Request-ID"] ?: request.headers["X-Correlation-ID"] ?: "-"

            "requestId=$requestId method=$method path=$path query=$query status=$status durationMs=$durationMs clientIp=$clientIp reqContentType=$reqContentType resContentType=$resContentType"
        }
    }
}

