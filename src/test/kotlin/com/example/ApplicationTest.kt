package com.example

import com.example.data.*
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

class ApplicationTest {
    private var orderRequest = defaultOrderRequest.copy()
    private var stockAggregationRequest = defaultStockAggregationRequest.copy()

    companion object {
        private val defaultOrderRequest = OrderRequest(
            side = "buy",
            type = "market",
            timeInForce = "day",
            quantity = "2",
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
            symbols = "AAPL",
            timeframe = "5Min",
            startDateTime = null,
            endDateTime = null,
            limit = 1000,
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
    fun testOrderSetAllParameter() = testApplication {
        application {
            module()
        }
        val httpResponse = setAllOrderParameter()
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertEquals(orderRequest, httpResponse.body())
    }

    private suspend fun setAllOrderParameter(): HttpResponse {
        val client = getClient()

        val httpResponse = client.post("/Order/SetAllParameter") {
            setBody(orderRequest)
        }
        client.close()
        return httpResponse
    }

    private suspend fun setAllHistBarsParameter(): HttpResponse {
        val client = getClient()
        val httpResponse = client.post("/HistoricalBars/SetAllParameter") {
            setBody(stockAggregationRequest)
        }
        client.close()
        return httpResponse
    }

    @Test
    fun testCreateOrder() = testApplication {
        application {
            module()
        }
        // Precondition
        orderRequest = defaultOrderRequest.copy()
        val preHttpResponse = setAllOrderParameter()
        assertEquals(HttpStatusCode.OK, preHttpResponse.status)

        val client = getClient()
        val httpResponse = client.get("/Order/Create")
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val response = httpResponse.body<OrderResponse>()
                assertEquals(HttpStatusCode.OK, httpResponse.status)
                assertEquals(orderRequest.quantity, response.qty)
            }
            else -> assert(false) // TODO: for now: Let test fail
        }
    }

    @Test
    fun testBadOrder() = testApplication {
        application {
            module()
        }
        // Precondition
        orderRequest = defaultOrderRequest.copy()
        orderRequest.symbol = "ASDFASDF"
        val preHttpResponse = setAllOrderParameter()
        assertEquals(HttpStatusCode.OK, preHttpResponse.status)
        assertEquals(orderRequest, preHttpResponse.body())

        val client = getClient()
        val httpResponse = client.get("/Order/Create")

        val testString = "Input parameters are not recognized."
        when (httpResponse.status) {
            HttpStatusCode.UnprocessableEntity -> {
                val response = httpResponse.bodyAsText()
                assertEquals(HttpStatusCode.UnprocessableEntity, httpResponse.status)
                assertEquals(response, testString);
            }
            else -> assert(false) // TODO: for now: Let test fail
        }


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
        stockAggregationRequest = defaultStockAggregationRequest.copy()
        val preHttpResponse = setAllHistBarsParameter()
        assertEquals(HttpStatusCode.OK, preHttpResponse.status)
        assertEquals(stockAggregationRequest, preHttpResponse.body())

        val client = getClient()

        val httpResponse = client.get("/HistoricalBars/Request")

        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val response = httpResponse.body<StockAggregationResponse>()
                assertEquals(HttpStatusCode.OK, httpResponse.status)
            }
            else -> assert(false) // TODO: for now: Let test fail
        }

    }

}
