package com.example

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
import org.example.finance.datamodel.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private var orderRequest = defaultOrderRequest

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
    fun testSetAllParameter() = testApplication {
        application {
            module()
        }
        val httpResponse = setAllParameter()
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertEquals(orderRequest, httpResponse.body())
    }

    private suspend fun setAllParameter(): HttpResponse {
        val client = getClient()

        val httpResponse = client.post("/Order/SetAllParameter") {
            setBody(orderRequest)
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
        orderRequest = defaultOrderRequest
        val preHttpResponse = setAllParameter()
        assertEquals(HttpStatusCode.OK, preHttpResponse.status)

        val client = getClient()
        val httpResponse = client.get("/Order/Create")
        println("httpResponse")
        println(httpResponse)
        println("ORDERREUEST")
        println(orderRequest)
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val response = httpResponse.body<OrderResponse>()
                assertEquals(HttpStatusCode.OK, httpResponse.status)
                assertEquals(orderRequest.quantity, response.filledQty)
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
}
