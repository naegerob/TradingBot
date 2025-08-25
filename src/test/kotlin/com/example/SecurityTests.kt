package com.example

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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        application {
            configureSerialization()
            //configureAuthentication()
            configureRouting()
        }

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
//                url {
//                    protocol = URLProtocol.HTTPS
//                }
            }
            assertEquals(OK, httpResponse.status)
            val loginResponse = httpResponse.body<LoginResponse>()
            assertEquals(loginResponse.refreshToken, "refreshToken")
            assertEquals(loginResponse.accessToken, "accessToken")
            assertNotNull(loginResponse.accessToken)
            assertNotNull(loginResponse.refreshToken)
        }
    }
}