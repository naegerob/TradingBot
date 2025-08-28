package com.example

import com.example.data.token.LoginRequest
import com.example.data.token.LoginResponse
import com.example.data.token.RefreshResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

class SecurityTests {

    @Test
    fun `Login should return access and refresh Token`() = testApplication {
        environment {
            config = ApplicationConfig("application.yaml")
        }

        val client = createConfiguredClient(this)
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

    @Test
    fun `Login with invalid credentials returns unauthorized`() = testApplication {
        environment {
            config = ApplicationConfig("application.yaml")
        }
        val client = createConfiguredClient(this)
        val loginRequest = LoginRequest(
            username = "wrong",
            password = "wrong"
        )
        listOf("/login", "/").forEach { path ->
            val httpResponse = client.post(path) {
                setBody(loginRequest)
            }
            assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
        }
    }

    @Test
    fun `Access protected endpoint with valid and invalid tokens`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createConfiguredClient(this)

        // Test both login endpoints
        listOf("/login", "/").forEach { loginPath ->
            val loginResponse = performLogin(client, loginPath)
            val accessToken = loginResponse.accessToken

            val protectedResponse = client.get("/AccountDetails") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            assertEquals(OK, protectedResponse.status)
        }

        // Access protected endpoint with invalid token
        val invalidTokenResponse = client.get("/AccountDetails") {
            header(HttpHeaders.Authorization, "Bearer invalid.token.value")
        }
        assertEquals(HttpStatusCode.Unauthorized, invalidTokenResponse.status)

        // Access protected endpoint with missing token
        val missingTokenResponse = client.get("/AccountDetails")
        assertEquals(HttpStatusCode.Unauthorized, missingTokenResponse.status)
    }

    @Test
    fun `Refresh token flow works and rejects invalid refresh tokens`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createConfiguredClient(this)

        // Test both login endpoints
        listOf("/login", "/").forEach { loginPath ->
            val loginResponse = performLogin(client, loginPath)
            val refreshToken = loginResponse.refreshToken
            val accessToken = loginResponse.accessToken

            val refreshResponse = client.post("/auth/refresh") {
                header(HttpHeaders.Authorization, "Bearer $refreshToken")
                setBody(mapOf("refreshToken" to refreshToken))
            }
            assertEquals(OK, refreshResponse.status)
            val newaccessToken = refreshResponse.body<RefreshResponse>()
            assertNotNull(newaccessToken.accessToken)
            assertTrue(isJwtFormat(newaccessToken.accessToken))
        }

        // Use invalid refresh token (no Authorization header)
        val invalidRefreshResponse = client.post("/auth/refresh") {
            setBody(mapOf("refreshToken" to "invalid.token.value"))
        }
        assertEquals(HttpStatusCode.Unauthorized, invalidRefreshResponse.status)
    }
}

private fun createConfiguredClient(app: ApplicationTestBuilder) = app.createClient {
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

private fun performLogin(client: HttpClient, path: String): LoginResponse = runBlocking {
    val username = System.getenv("AUTHENTIFICATION_USERNAME")
    val password = System.getenv("AUTHENTIFICATION_PASSWORD")
    val loginRequest = LoginRequest(username = username, password = password)
    val response = client.post(path) { setBody(loginRequest) }
    assertEquals(OK, response.status)
    response.body()
}

private fun isJwtFormat(token: String): Boolean {
    val parts = token.split(".")
    return parts.size == 3 && parts.all { it.isNotBlank() }
}
