package com.example

import com.example.data.alpaca.StockAggregationRequest
import com.example.data.singleModels.LoginRequest
import com.example.data.singleModels.LoginResponse
import com.example.tradinglogic.BacktestConfig
import com.example.tradinglogic.BotConfig
import com.example.tradinglogic.StopLoss
import com.example.tradinglogic.Strategies
import com.example.tradinglogic.TakeProfit
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
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

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = false
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
        install(DefaultRequest) {
            header("content-type", "application/json")
            header("accept", "application/json")
        }
    }

    @Test
    fun `Start Bot with Configured and returns OK`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

        val botConfig = BotConfig(
            symbols = "AAPL",
            positionSize = 100.0,
            timeframe = "1H",
            numberSamples = 10,
            orderClass = "simple",
            orderType = "market",
            strategySelection = Strategies.MovingAverage,
            takeProfit = TakeProfit(limitPrice = "150.0"),
            stopLoss = StopLoss(stopPrice = "140.0", limitPrice = "139.0")
        )

        val accessToken = loginAndGetToken(client)
        val httpResponseConfig = client.post("/Bot/Config") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(botConfig)
        }
        assertEquals(OK, httpResponseConfig.status)

        val httpResponseStart = client.get("/Bot/Start") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assertEquals(OK, httpResponseStart.status)
    }


    @Test
    fun `Start Bot endpoint returns OK`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

        val accessToken = loginAndGetToken(client)
        val httpResponse = client.get("/Bot/Start") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assertEquals(BadRequest, httpResponse.status)
    }

    @Test
    fun `Stop Bot endpoint returns Bad Request because it is not started`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

        val accessToken = loginAndGetToken(client)
        val httpResponse = client.get("/Bot/Stop") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assertEquals(BadRequest, httpResponse.status)
    }

    @Test
    fun `Backtesting endpoint with valid config returns OK`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val accessToken = loginAndGetToken(client)
        val httpResponse = client.post("/Bot/Backtesting") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(backtestConfig)
        }
        assertEquals(OK, httpResponse.status)
    }

    @Test
    fun `Historical Bars Get endpoint returns result`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

        val accessToken = loginAndGetToken(client)
        val httpResponse = client.get("/HistoricalBars/Get") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assert(httpResponse.status == OK)
        assert(httpResponse.bodyAsText().isNotEmpty())
    }
}
