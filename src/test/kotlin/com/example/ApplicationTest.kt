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
import org.example.finance.datamodel.OrderRequest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val orderRequest = OrderRequest(
        side = "buy",
        type = "market",
        timeInForce = "day", // Good 'til canceled
        quantity = "2",
        symbol = "TSLA",
        limitPrice = "",
        stopPrice = "",
        trailPrice = "",
        trailPercent = "",
        extendedHours = false,
        clientOrderId = "",
        orderClass = "",
        takeProfit = null,
        stopLoss = null,
        positionIntent = ""
    )

    private fun getClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                header("Content-Type", "application/json")
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
            contentType(Json)
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
        assertEquals(HttpStatusCode.OK, httpResponse.status)

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
