package com.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.example.finance.datamodel.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val orderRequest = OrderRequest(
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
        println("httpResponse")
        println(httpResponse)
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val response = httpResponse.body<OrderResponse>()
                assertEquals(HttpStatusCode.OK, httpResponse.status)
                assertEquals(orderRequest.quantity, response.filledQty)
            }
            HttpStatusCode.UnprocessableEntity -> {
                assertEquals(HttpStatusCode.UnprocessableEntity, httpResponse.status)
                assertEquals("Input parameters are not recognized.", httpResponse.body())
            }
            HttpStatusCode.Forbidden -> {
                assertEquals(HttpStatusCode.Forbidden, httpResponse.status)
                assertEquals("Buying power or shares is not sufficient.", httpResponse.body())
            }
            HttpStatusCode.InternalServerError -> {
                assertEquals(HttpStatusCode.InternalServerError, httpResponse.status)
                assertEquals("Error is not handled.", httpResponse.body())
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
        val accountId = "PA3JEJBVNXV0"
        val state = "ACTIVE"
        val httpResponse = client.get("/AccountDetails")
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val accountDetails = httpResponse.body<Account>()
        assertEquals(accountId, accountDetails.accountNumber)
        assertEquals(state, accountDetails.status)
    }
}
