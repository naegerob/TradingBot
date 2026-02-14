package com.example.configuration

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.response.respond
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiter() {
    install(RateLimit) {
        register(RateLimitName("login")) {
            rateLimiter(
                limit = 30,
                refillPeriod = 2.seconds
            )
        }

        onLimitExceeded { call, _ ->
            call.respond(HttpStatusCode.TooManyRequests, "Too many login requests")
        }
    }
}