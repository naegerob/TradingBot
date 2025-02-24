package com.example

import com.example.data.singleModels.*
import com.example.tradingLogic.BacktestConfig
import com.example.tradingLogic.BacktestResult
import com.example.tradingLogic.strategies.Strategies
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApplicationTest {

    companion object {
        private val defaultOrderRequest = OrderRequest(
            side = "buy",
            type = "market",
            timeInForce = "day",
            quantity = null,
            notional = "20",
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
        private val defaultStockAggregationRequest = StockAggregationRequest(
            symbols = "TSLA",
            timeframe = "1H",
            startDateTime = "2024-12-01T00:00:00Z",
            endDateTime = "2025-02-02T00:00:00Z",
            limit = 500,
            adjustment = "raw",
            asOfDate = null,
            feed = "sip",
            currency = "USD",
            pageToken = null,
            sort = "asc"
        )
    }

    private fun getClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = false
                    encodeDefaults = true
                })
            }
            defaultRequest {
                header("content-type", "application/json")
                header("accept", "application/json")
                url {
                    protocol = URLProtocol.HTTP
                    host = "127.0.0.1"
                    port = 8080
                }
            }
        }
    }


    @Test
    fun testBadHistoricalSetAllParameter() = testApplication {
        application {
            module()
        }
        val client = getClient()
        val stockAggregationRequest :StockAggregationRequest = defaultStockAggregationRequest
        stockAggregationRequest.symbols = ""
        val httpResponse = client.post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }
        println(httpResponse.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun testCreateOrder() = testApplication {
        application {
            module()
        }
        // Precondition
        val orderRequest = defaultOrderRequest.copy()

        val client = getClient()
        val httpResponse = client.post("/Order/Create") {
            setBody(orderRequest)
        }
        val response = httpResponse.body<OrderResponse>()
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertEquals(orderRequest.quantity, response.qty)

    }

    @Test
    fun testCreateBadOrder() = testApplication {
        application {
            module()
        }
        // Precondition
        val orderRequest = defaultOrderRequest.copy()
        orderRequest.symbol = ""
        println(orderRequest)

        val client = getClient()
        val httpResponse = client.post("/Order/Create") {
            setBody(orderRequest)
        }

        val response = httpResponse.bodyAsText()
        println(response)
        assertEquals(HttpStatusCode.UnprocessableEntity, httpResponse.status)

    }

    @Test
    fun testAccountDetails() = testApplication {
        application {
            module()
        }
        val client = getClient()
        val accountId = "PA3ALX4NGLN0"
        val state = "ACTIVE"
        val httpResponse = client.get("/AccountDetails")

        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val accountDetails = httpResponse.body<Account>()
        assertEquals(accountId, accountDetails.accountNumber)
        assertEquals(state, accountDetails.status)
    }

    @Test
    fun testHistBarsRequest() = testApplication {
        application {
            module()
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()

        val client = getClient()
        val httpResponse = client.post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }

        val response = httpResponse.body<StockAggregationResponse>()
        println(response)
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertNotEquals(emptyMap(), response.bars)
    }

    @Test
    fun testBadHistBarsRequest() = testApplication {
        application {
            module()
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        stockAggregationRequest.symbols = ""

        val client = getClient()
        val httpResponse = client.post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun testBacktesting() = testApplication {
        application {
            module()
        }
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        val strategySelector = Strategies.MovingAverage

        val backtestConfig = BacktestConfig(strategySelector, stockAggregationRequest)


        val client = getClient()
        val httpResponse = client.post("/Bot/Backtesting") {
            setBody(backtestConfig)
        }
        val backtestResult = httpResponse.body<BacktestResult>()
        backtestResult.let {
            println(it.strategyName)
            println(it.winRate)
            println(it.avgProfit)
            println(it.totalProfit)
        }
    }
    @Test
    fun testStartAndStopBot() = testApplication {
        application {
            module()
        }

        val client = getClient()

        var httpResponse = client.get("/Bot/Start")
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        httpResponse = client.get("/Bot/Stop")
        assertEquals(HttpStatusCode.OK, httpResponse.status)
    }

}
