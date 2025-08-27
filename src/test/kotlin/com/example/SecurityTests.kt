package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.token.LoginRequest
import com.example.data.token.LoginResponse
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.config.yaml.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class SecurityTests {

    @AfterTest
    fun tearDown() {
       // Write something smart
    }

    @Test
    fun `Login should return access and refresh Token`() = testApplication {
        environment {
            config = ApplicationConfig("application.yaml")
        }
        // calling modules and configuration is not needed, since it is automatically installed from yaml

        // Precondition
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = false
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(DefaultRequest) {
                header("content-type", "application/json")
                header("accept", "application/json")
            }
        }
        val username = System.getenv("AUTHENTIFICATION_USERNAME")
        val password = System.getenv("AUTHENTIFICATION_PASSWORD")
        val loginRequest = LoginRequest(
            username = username,
            password = password
        )

        listOf("/login", "/").forEach { path ->
            val httpResponse = client.post(path) {
                setBody(loginRequest)
            }
            assertEquals(OK, httpResponse.status)
            val loginResponse = httpResponse.body<LoginResponse>()
            assertNotNull(loginResponse.accessToken)
            assertNotNull(loginResponse.refreshToken)
            assertTrue(isJwtFormat(loginResponse.accessToken))
            assertTrue(isJwtFormat(loginResponse.refreshToken))
        }
    }
}

private fun isJwtFormat(token: String): Boolean {
    val parts = token.split(".")
    return parts.size == 3 && parts.all { it.isNotBlank() }
}