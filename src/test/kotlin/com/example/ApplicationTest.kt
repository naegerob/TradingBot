package com.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.server.util.*
import org.example.finance.datamodel.Account
import org.example.finance.datamodel.OrderRequest
import org.example.finance.datamodel.StopLoss
import org.example.finance.datamodel.TakeProfit
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testSetAllParameter() = testApplication {
        application {
            module()
        }
        val client = HttpClient {
            install(ContentNegotiation) {
                json()  // Include necessary configurations for JSON
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTP
                    host = "127.0.0.1"
                    port = 8080
                }
            }
        }

        val orderRequest = OrderRequest(
            side = "sell",
            type = "limit",
            timeInForce = "gtc", // Good 'til canceled
            quantity = "10",
            symbol = "TSLA",
            limitPrice = "800.50",
            stopPrice = "780.00",
            trailPrice = null,
            trailPercent = "1.5",
            extendedHours = true,
            clientOrderId = "custom_order_12345",
            orderClass = "bracket",
            takeProfit = TakeProfit(limitPrice = "850.00"),
            stopLoss = StopLoss(stopPrice = "770.00"),
            positionIntent = "long"
        )

        val httpResponse = client.post("/Order/SetAllParameter") {
            contentType(ContentType.Application.Json)
            setBody(orderRequest)
        }
        assertEquals(httpResponse.status, HttpStatusCode.OK)
        assertEquals(httpResponse.body(), orderRequest)
    }

    @Test
    fun testAccountDetails() = testApplication {
        application {
            module()
        }
        val client = HttpClient {
            install(ContentNegotiation) {
                json()  // Include necessary configurations for JSON
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTP
                    host = "127.0.0.1"
                    port = 8080
                }
            }
        }
        val accountId = "PA3JEJBVNXV0"
        val state = "ACTIVE"
        val httpResponse = client.get("/AccountDetails")
        assertEquals(httpResponse.status, HttpStatusCode.OK)
        val accountDetails = httpResponse.body<Account>()
        assertEquals(accountDetails.accountNumber, accountId)
        assertEquals(accountDetails.status, state)

    }

}
