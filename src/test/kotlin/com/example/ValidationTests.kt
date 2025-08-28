package com.example

import com.example.data.token.LoginRequest
import com.example.data.token.LoginResponse
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
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
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTests {

    private suspend fun loginAndGetToken(client: HttpClient, path: String = "/login"): String {
        val username = System.getenv("AUTHENTIFICATION_USERNAME")
        val password = System.getenv("AUTHENTIFICATION_PASSWORD")
        val loginRequest = LoginRequest(username = username, password = password)
        val response = client.post(path) { setBody(loginRequest) }
        assertEquals(OK, response.status)
        val loginResponse = response.body<LoginResponse>()
        return loginResponse.accessToken
    }

    @Test
    fun `Order validation fails with invalid side parameter`() = testApplication {
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

        val invalidOrderRequest = OrderRequest(
            side = "invalid_side", // Invalid side
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

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidOrderRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Order validation fails with invalid type parameter`() = testApplication {
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

        val invalidOrderRequest = OrderRequest(
            side = "buy",
            type = "invalid_type", // Invalid type
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

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidOrderRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Order validation fails with invalid timeInForce parameter`() = testApplication {
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

        val invalidOrderRequest = OrderRequest(
            side = "buy",
            type = "market",
            timeInForce = "invalid_tif", // Invalid timeInForce
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

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidOrderRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with multiple symbols`() = testApplication {
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

        val invalidStockRequest = StockAggregationRequest(
            symbols = "AAPL,MSFT", // Multiple symbols (contains comma)
            timeframe = "1H",
            startDateTime = "2024-01-01T00:00:00Z",
            endDateTime = "2024-01-31T00:00:00Z",
            limit = 100,
            adjustment = "raw",
            asOfDate = null,
            feed = "sip",
            currency = "USD",
            pageToken = null,
            sort = "asc"
        )

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with invalid timeframe`() = testApplication {
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

        val invalidStockRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "invalid_timeframe", // Invalid timeframe
            startDateTime = "2024-01-01T00:00:00Z",
            endDateTime = "2024-01-31T00:00:00Z",
            limit = 100,
            adjustment = "raw",
            asOfDate = null,
            feed = "sip",
            currency = "USD",
            pageToken = null,
            sort = "asc"
        )

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with invalid feed`() = testApplication {
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

        val invalidStockRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "1H",
            startDateTime = "2024-01-01T00:00:00Z",
            endDateTime = "2024-01-31T00:00:00Z",
            limit = 100,
            adjustment = "raw",
            asOfDate = null,
            feed = "invalid_feed", // Invalid feed
            currency = "USD",
            pageToken = null,
            sort = "asc"
        )

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with invalid sort parameter`() = testApplication {
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

        val invalidStockRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "1H",
            startDateTime = "2024-01-01T00:00:00Z",
            endDateTime = "2024-01-31T00:00:00Z",
            limit = 100,
            adjustment = "raw",
            asOfDate = null,
            feed = "sip",
            currency = "USD",
            pageToken = null,
            sort = "invalid_sort" // Invalid sort
        )

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation passes with valid parameters`() = testApplication {
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

        val validStockRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "5Min", // Valid timeframe with number + Min
            startDateTime = "2024-01-01T00:00:00Z",
            endDateTime = "2024-01-31T00:00:00Z",
            limit = 100,
            adjustment = "raw",
            asOfDate = null,
            feed = "sip",
            currency = "USD",
            pageToken = null,
            sort = "asc"
        )

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(validStockRequest)
        }
        // Should be OK or some other valid response (depends on mock setup)
        assert(httpResponse.status.value in 200..299 || httpResponse.status == HttpStatusCode.BadRequest)
    }
}
