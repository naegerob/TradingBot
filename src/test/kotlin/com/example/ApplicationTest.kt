package com.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.example.finance.datamodel.Account
import org.example.finance.datamodel.ErrorResponse
import org.example.finance.datamodel.OrderRequest
import org.example.finance.datamodel.OrderResponse
import org.junit.jupiter.api.BeforeEach
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val orderRequest = OrderRequest(
        side = "buy",
        type = "market",
        timeInForce = "day",
        quantity = "2",
        symbol = "TSLA",
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

    private fun getClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
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
        val client = getClient()

        val httpResponse = client.post("/Order/SetAllParameter") {
            setBody(orderRequest)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertEquals(orderRequest, httpResponse.body())
    }

    @Test
    fun testCreateOrder() = testApplication {
        application {
            module()
        }
        val client = getClient()
        val httpResponse = client.get("/Order/Create")

        val accountDetails = httpResponse.body<OrderResponse>()
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertEquals(orderRequest.quantity, accountDetails.qty)
    }

    @Test
    fun testCreateBadOrder() = testApplication {
        application {
            module()
        }
        orderRequest.symbol = "AAPL"
        orderRequest.side = "buy"
        orderRequest.type = "limit"
        orderRequest.timeInForce = "gtc"

        val client = getClient()
        val httpResponseSetParameter = client.post("/Order/SetAllParameter") {
            setBody(orderRequest)
        }
        println("orderRequest")
        println(orderRequest)
        assertEquals(HttpStatusCode.OK, httpResponseSetParameter.status)
        assertEquals(orderRequest, httpResponseSetParameter.body())

        val httpResponseCreate = client.get("/Order/Create")
        val accountDetails = httpResponseCreate.body<ErrorResponse>()
        assertEquals(HttpStatusCode.OK, httpResponseCreate.status)
        assertEquals(true, accountDetails.message.contains("limit"))

    }

    @Test
    fun testAccountDetails() = testApplication {
        application {
            module()
        }

        val client = getClient()
        val accountId = "PA3JEJBVNXV0"
        val state = "ACTIVE"
        val httpResponse = client.get("/AccountDetails")
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val accountDetails = httpResponse.body<Account>()
        assertEquals(accountId, accountDetails.accountNumber)
        assertEquals(state, accountDetails.status)
    }


}
