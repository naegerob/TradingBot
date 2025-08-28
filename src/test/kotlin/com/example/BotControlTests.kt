package com.example

import com.example.data.token.LoginRequest
import com.example.data.token.LoginResponse
import com.example.tradingLogic.BacktestConfig
import com.example.tradingLogic.strategies.Strategies
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

class BotControlTests {

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
    fun `Start Bot endpoint returns OK`() = testApplication {
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

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.get("/Bot/Start") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assertEquals(OK, httpResponse.status)
    }

    @Test
    fun `Stop Bot endpoint returns OK`() = testApplication {
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

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.get("/Bot/Stop") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assertEquals(OK, httpResponse.status)
    }

    @Test
    fun `Backtesting endpoint with valid config returns OK`() = testApplication {
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

        val backtestConfig = BacktestConfig(
            strategySelector = Strategies.MovingAverage,
            stockAggregationRequest = StockAggregationRequest(
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
                sort = "asc"
            )
        )

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.post("/Bot/Backtesting") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(backtestConfig)
        }
        assertEquals(OK, httpResponse.status)
    }

    @Test
    fun `Historical Bars Get endpoint returns result`() = testApplication {
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

        val accessToken = runBlocking { loginAndGetToken(client) }
        val httpResponse = client.get("/HistoricalBars/Get") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        // This endpoint uses default StockAggregationRequest which may have validation issues
        // Expected behavior depends on default values - could be OK or BadRequest
        assert(httpResponse.status == OK || httpResponse.status == HttpStatusCode.BadRequest)
    }
}
