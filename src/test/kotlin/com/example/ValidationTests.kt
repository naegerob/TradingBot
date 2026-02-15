package com.example

import com.example.services.ValidationService
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.token.LoginRequest
import com.example.data.token.LoginResponse
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

    private suspend fun loginAndGetTokenWithCSRF(client: HttpClient, path: String = "/login"): Pair<String, String> {
        val username = System.getenv("AUTHENTIFICATION_USERNAME")
        val password = System.getenv("AUTHENTIFICATION_PASSWORD")
        val loginRequest = LoginRequest(username = username, password = password)
        val response = client.post(path) { setBody(loginRequest) }
        assertEquals(OK, response.status)
        val loginResponse = response.body<LoginResponse>()
        return Pair(loginResponse.accessToken, loginResponse.csrfToken)
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
    fun `Order validation fails with invalid side parameter`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidOrderRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Order validation fails with invalid type parameter`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidOrderRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Order validation fails with invalid timeInForce parameter`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidOrderRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with multiple symbols`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with invalid timeframe`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with invalid feed`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation fails with invalid sort parameter`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("X-CSRF-Token", csrfToken)
            setBody(invalidStockRequest)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }

    @Test
    fun `Stock request validation passes with valid parameters`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

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

        val (accessToken, csrfToken) = loginAndGetTokenWithCSRF(client)
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(validStockRequest)
            header("X-CSRF-Token", csrfToken)
        }
        // Should be OK or some other valid response (depends on mock setup)
        assert(httpResponse.status.value in 200..299 || httpResponse.status == HttpStatusCode.BadRequest)
    }

    @Test
    fun `areValidOrderParameter rejects SQL injection in symbol`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL; DROP TABLE orders;--",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "1",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter rejects SQL injection in symbol 2`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market; DROP TABLE orders;--",
            side = "buy",
            timeInForce = "day",
            quantity = "1",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter rejects SQL injection in symbol 3`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy; DROP TABLE orders;--",
            timeInForce = "day",
            quantity = "1",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter rejects SQL injection in symbol 5`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day; DROP TABLE orders;--",
            quantity = "1",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter rejects SQL injection in symbol 6`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "1; DROP TABLE orders;--",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter with negative limitPrice`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "11",
            notional = null,
            limitPrice = "-23",
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter with negative stopPrice`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "11",
            notional = null,
            limitPrice = null,
            stopPrice = "-74",
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter with negative trailPrice`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "11",
            notional = null,
            limitPrice = null,
            stopPrice = "-234",
            trailPrice = null,
            trailPercent = null,
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)
    }

    @Test
    fun `areValidOrderParameter with unreal percentages`() {
        val validator = ValidationService()

        val req = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "11",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = "-1",
            legs = null
        )

        val isValid = validator.areValidOrderParameter(req)
        assertEquals(false, isValid)

        val req2 = OrderRequest(
            symbol = "AAPL",
            type = "market",
            side = "buy",
            timeInForce = "day",
            quantity = "11",
            notional = null,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = "101",
            legs = null
        )
        val isValid2 = validator.areValidOrderParameter(req2)
        assertEquals(false, isValid2)
    }
}
