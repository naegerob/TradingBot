package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.time.Instant

fun Application.configureAuthentication() {
    val jwtConfig = environment.config.config("jwt")
    val issuer = jwtConfig.property("issuer").getString()
    val audience = jwtConfig.property("audience").getString()
    val myRealm = jwtConfig.property("realm").getString()
    val publicKeyPath = jwtConfig.property("publicKeyPath").getString()

    val publicKey = loadRSAPublicKey(publicKeyPath)
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(
                JWT
                    .require(Algorithm.RSA256(publicKey, null)) // only public key needed for verification
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                // Neu: nur Access Token zulassen
                val type = credential.payload.getClaim("type")?.asString()
                if (type != "access") return@validate null
                if (credential.payload.audience.contains(audience))
                    JWTPrincipal(credential.payload)
                else
                    null
            }
            challenge { _, _ ->
                try {
                    val authHeader = call.request.headers["Authorization"]
                    val rawToken = authHeader?.removePrefix("Bearer ")?.trim()
                    val state = analyzeToken(rawToken, skewSeconds = 5)

                    when (state) {
                        TokenState.EXPIRED -> call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "TOKEN_EXPIRED", "message" to "Access token expired. Please use the refresh token.")
                        )
                        TokenState.MALFORMED -> call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "TOKEN_MALFORMED", "message" to "Token structure invalid (malformed).")
                        )
                        TokenState.MISSING -> call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "TOKEN_MISSING", "message" to "Authorization header missing.")
                        )
                        TokenState.VALID_OR_UNKNOWN -> call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "TOKEN_INVALID", "message" to "Token is invalid or signature verification failed.")
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "AUTH_FAILURE", "message" to "Authentication handling failed.")
                    )
                }
            }
        }
    }
}

// Distinguishes token conditions to avoid throwing exceptions on tampered tokens.
private enum class TokenState { MISSING, MALFORMED, EXPIRED, VALID_OR_UNKNOWN }

private fun analyzeToken(rawToken: String?, skewSeconds: Long = 5): TokenState {
    if (rawToken.isNullOrBlank()) return TokenState.MISSING
    return try {
        val decoded = JWT.decode(rawToken)
        val expDate = decoded.expiresAt
        if (expDate != null) {
            val now = Instant.now()
            val expInstant = expDate.toInstant()
            if (expInstant.plusSeconds(skewSeconds).isBefore(now)) {
                return TokenState.EXPIRED
            }
        }
        TokenState.VALID_OR_UNKNOWN // could still be invalid signature; verifier already failed earlier
    } catch (_: Exception) {
        TokenState.MALFORMED
    }
}
