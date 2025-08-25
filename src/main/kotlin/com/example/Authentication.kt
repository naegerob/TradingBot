package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.loadRSAPublicKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

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
                if (credential.payload.audience.contains(audience))
                    JWTPrincipal(credential.payload)
                else
                    null
            }
            challenge { _,_ ->
                call.respondText("Token is not valid or has expired, you need to login first.", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}


