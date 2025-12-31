package com.example

import com.example.data.singleModels.OrderRequest
import com.example.data.token.LoginRequest
import com.example.data.token.RefreshRequest
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthenticationEdgeCaseTests {

    @Test
    fun `Access protected endpoint without token returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val httpResponse = client.get("/AccountDetails")
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `Access protected endpoint with malformed token returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val httpResponse = client.get("/AccountDetails") {
            header(HttpHeaders.Authorization, "Bearer malformed.token")
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `Access protected endpoint with empty Bearer token returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val httpResponse = client.get("/AccountDetails") {
            header(HttpHeaders.Authorization, "Bearer ")
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `Access protected endpoint with wrong auth scheme returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val httpResponse = client.get("/AccountDetails") {
            header(HttpHeaders.Authorization, "Basic sometoken")
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `Login with empty username returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val loginRequest = LoginRequest(username = "", password = "somepassword")
        val httpResponse = client.post("/login") {
            setBody(loginRequest)
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `Login with empty password returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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
        val loginRequest = LoginRequest(username = username, password = "")
        val httpResponse = client.post("/login") {
            setBody(loginRequest)
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `Refresh token with invalid token returns Unauthorized`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val refreshRequest = RefreshRequest(refreshToken = "invalid.refresh.token")
        val httpResponse = client.post("/auth/refresh") {
            header(HttpHeaders.Authorization, "Bearer invalid.token")
            setBody(refreshRequest)
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `All protected endpoints require authentication - Order Create`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val orderRequest = OrderRequest(
            side = "buy",
            type = "market",
            timeInForce = "day",
            quantity = "10",
            notional = null,
            symbol = "AAPL",
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            extendedHours = false,
            clientOrderId = null,
            orderClass = null,
            takeProfit = null,
            stopLoss = null,
            positionIntent = null
        )

        val httpResponse = client.post("/Order/Create") {
            setBody(orderRequest)
        }
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `All protected endpoints require authentication - Bot Start`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val httpResponse = client.get("/Bot/Start")
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `All protected endpoints require authentication - Bot Stop`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val httpResponse = client.get("/Bot/Stop")
        assertEquals(HttpStatusCode.Unauthorized, httpResponse.status)
    }

    @Test
    fun `All indicator endpoints require authentication`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val indicatorEndpoints = listOf(
            "/Indicators/Original",
            "/Indicators/Support",
            "/Indicators/Resistance",
            "/Indicators/Sma/Short",
            "/Indicators/Sma/Long",
            "/Indicators/BollingerBands/Middle",
            "/Indicators/BollingerBands/Upper",
            "/Indicators/BollingerBands/Lower",
            "/Indicators/Rsi"
        )

        indicatorEndpoints.forEach { endpoint ->
            val httpResponse = client.get(endpoint)
            assertEquals(
                HttpStatusCode.Unauthorized,
                httpResponse.status,
                "Endpoint $endpoint should require authentication"
            )
        }
    }

    @Test
    fun `All backtest indicator endpoints require authentication`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

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

        val backtestEndpoints = listOf(
            "/BacktestIndicators/Original",
            "/BacktestIndicators/Support",
            "/BacktestIndicators/Resistance",
            "/BacktestIndicators/Sma/Short",
            "/BacktestIndicators/Sma/Long",
            "/BacktestIndicators/BollingerBands/Middle",
            "/BacktestIndicators/BollingerBands/Upper",
            "/BacktestIndicators/BollingerBands/Lower",
            "/BacktestIndicators/Rsi"
        )

        backtestEndpoints.forEach { endpoint ->
            val httpResponse = client.get(endpoint)
            assertEquals(
                HttpStatusCode.Unauthorized,
                httpResponse.status,
                "Endpoint $endpoint should require authentication"
            )
        }
    }
}
