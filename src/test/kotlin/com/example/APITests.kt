package com.example

import com.example.data.singleModels.*
import com.example.data.token.LoginRequest
import com.example.data.token.LoginResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.UnprocessableEntity
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class APITests : KoinTest {
    companion object {
        private val defaultOrderRequest = OrderRequest(
            side = "buy",
            type = "market",
            timeInForce = "day",
            quantity = "20",
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
        private val defaultStockAggregationRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "1H",
            startDateTime = "2024-01-01T00:00:00Z",
            endDateTime = "2025-02-02T00:00:00Z",
            limit = 1000,
            adjustment = "raw",
            asOfDate = null,
            feed = "sip",
            currency = "USD",
            pageToken = null,
            sort = "asc"
        )
    }

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
    fun `All Backtest Indicators`() = testApplication {
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
        val endpoints = listOf(
            "/BacktestIndicators/Original",
            "/BacktestIndicators/Support",
            "/BacktestIndicators/Resistance",
            "/BacktestIndicators/Sma/Short",
            "/BacktestIndicators/Sma/Long",
            "/BacktestIndicators/BollingerBands/Middle",
            "/BacktestIndicators/BollingerBands/Upper",
            "/BacktestIndicators/BollingerBands/Lower",
            "/BacktestIndicators/Rsi"
        )

        endpoints.forEach { endpoint ->
            val httpResponse = client.get(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            assertEquals(OK, httpResponse.status)
        }
    }

    @Test
    fun `All Normal Indicators`() = testApplication {
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
        val endpoints = listOf(
            "/Indicators/Original",
            "/Indicators/Support",
            "/Indicators/Resistance",
            "/Indicators/Sma/Short",
            "/Indicators/Sma/Long",
            "/Indicators/BollingerBands/Middle",
            "/Indicators/BollingerBands/Upper",
            "/Indicators/BollingerBands/Lower",
            "/Indicators/Rsi"
        )

        endpoints.forEach { endpoint ->
            val httpResponse = client.get(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            assertEquals(OK, httpResponse.status)
        }
    }

    @Test
    fun `Create a Good Order to Alpaca`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockOrderResponse = OrderResponse(
            id = "64c48451-34c4-4642-83a8-43f5f75bf6fd",
            clientOrderId = "2b3b5479-923b-4b5e-a95c-00e0b8d0280a",
            createdAt = "2025-03-23T20:09:23.265233813Z",
            updatedAt = "2025-03-23T20:09:23.266033472Z",
            submittedAt = "2025-03-23T20:09:23.265233813Z",
            filledAt = null,
            expiredAt = null,
            canceledAt = null,
            failedAt = null,
            replacedAt = null,
            replacedBy = null,
            replaces = null,
            assetId = "b0b6dd9d-8b9b-48a9-ba46-b9d54906e415",
            symbol = "AAPL",
            assetClass = "us_equity",
            notional = null,
            qty = "20",
            filledQty = "0",
            filledAvgPrice = null,
            orderClass = "",
            orderType = "market",
            type = "market",
            side = "buy",
            positionIntent = "buy_to_open",
            timeInForce = "day",
            limitPrice = null,
            stopPrice = null,
            status = "accepted",
            extendedHours = false,
            legs = null,
            trailPercent = null,
            trailPrice = null,
            hwm = null,
            subtag = null,
            source = null,
            expiresAt = "2025-03-24T20:00:00Z"
        )
        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockOrderResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }

        val orderRequest = defaultOrderRequest.copy()
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
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(orderRequest)
        }
        val response = httpResponse.body<OrderResponse>()
        assertEquals(OK, httpResponse.status)
        assertEquals(orderRequest.quantity, response.qty)
        assertEquals(orderRequest.symbol, response.symbol)
        assertEquals(mockOrderResponse.symbol, response.symbol)
        assertEquals(mockOrderResponse.qty, response.qty)

        unloadKoinModules(overrides)
    }

    @Test
    fun `Create a Bad Order to Alpaca`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockOrderResponse = OrderResponse(
            id = "64c48451-34c4-4642-83a8-43f5f75bf6fd",
            clientOrderId = "2b3b5479-923b-4b5e-a95c-00e0b8d0280a",
            createdAt = "2025-03-23T20:09:23.265233813Z",
            updatedAt = "2025-03-23T20:09:23.266033472Z",
            submittedAt = "2025-03-23T20:09:23.265233813Z",
            filledAt = null,
            expiredAt = null,
            canceledAt = null,
            failedAt = null,
            replacedAt = null,
            replacedBy = null,
            replaces = null,
            assetId = "b0b6dd9d-8b9b-48a9-ba46-b9d54906e415",
            symbol = "AAPL",
            assetClass = "us_equity",
            notional = null,
            qty = "2",
            filledQty = "0",
            filledAvgPrice = null,
            orderClass = "",
            orderType = "market",
            type = "market",
            side = "buy",
            positionIntent = "buy_to_open",
            timeInForce = "day",
            limitPrice = null,
            stopPrice = null,
            status = "accepted",
            extendedHours = false,
            legs = null,
            trailPercent = null,
            trailPrice = null,
            hwm = null,
            subtag = null,
            source = null,
            expiresAt = "2025-03-24T20:00:00Z"
        )

        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockOrderResponse),
                status = UnprocessableEntity,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }

        val orderRequest = defaultOrderRequest.copy()
        orderRequest.symbol = ""
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
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(orderRequest)
        }
        assertEquals(BadRequest, httpResponse.status)

        unloadKoinModules(overrides)
    }

    @Test
    fun `Get Account call to Alpaca`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockAccountResponse = Account(
            id = "8aa6e3cd-a6c7-41fe-8280-7bc70f11afce",
            adminConfigurations = emptyMap(),
            userConfigurations = null,
            accountNumber = "PA3ALX4NGLN0",
            status = "ACTIVE",
            cryptoStatus = "ACTIVE",
            optionsApprovedLevel = 3,
            optionsTradingLevel = 3,
            currency = "USD",
            buyingPower = "165882.23",
            regtBuyingPower = "165882.23",
            dayTradingBuyingPower = "0",
            effectiveBuyingPower = "165882.23",
            nonMarginableBuyingPower = "80941.11",
            optionsBuyingPower = "80941.11",
            bodDtbp = "0",
            cash = "67696.08",
            accruedFees = "0",
            portfolioValue = "98206.15",
            patternDayTrader = false,
            tradingBlocked = false,
            transfersBlocked = false,
            accountBlocked = false,
            createdAt = "2025-01-22T15:48:41.002938Z",
            tradeSuspendedByUser = false,
            multiplier = "2",
            shortingEnabled = true,
            equity = "98206.15",
            lastEquity = "98206.15111431422",
            longMarketValue = "30510.07",
            shortMarketValue = "0",
            positionMarketValue = "30510.07",
            initialMargin = "15265.04",
            maintenanceMargin = "9153.02",
            lastMaintenanceMargin = "9153.02",
            sma = "97623.26",
            dayTradeCount = 0,
            balanceAsOf = "2025-03-21",
            cryptoTier = 1,
            intradayAdjustments = "0",
            pendingRegTafFees = "0"
        )
        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockAccountResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }

        val accountId = "PA3ALX4NGLN0"
        val state = "ACTIVE"
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
        val httpResponse = client.get("/AccountDetails") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(OK, httpResponse.status)
        val accountDetails = httpResponse.body<Account>()
        assertEquals(accountId, accountDetails.accountNumber)
        assertEquals(state, accountDetails.status)

        unloadKoinModules(overrides)
    }

    @Test
    fun `Get Historical Bars Request with good parameter`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockStockAggregationResponse = StockAggregationResponse(
            bars = mapOf(
                "AAPL" to listOf(
                    StockBar(
                        close = 185.31,
                        high = 185.31,
                        low = 185.31,
                        trades = 28,
                        open = 185.31,
                        timestamp = "2024-01-03T00:00:00Z",
                        volume = 1045,
                        vwap = 185.31
                    ), StockBar(
                        close = 185.29,
                        high = 185.29,
                        low = 185.29,
                        trades = 36,
                        open = 185.29,
                        timestamp = "2024-01-03T00:01:00Z",
                        volume = 283,
                        vwap = 185.29
                    ), StockBar(
                        close = 185.29,
                        high = 185.29,
                        low = 185.29,
                        trades = 26,
                        open = 185.29,
                        timestamp = "2024-01-03T00:02:00Z",
                        volume = 381,
                        vwap = 185.29
                    ), StockBar(
                        close = 185.26,
                        high = 185.26,
                        low = 185.26,
                        trades = 30,
                        open = 185.26,
                        timestamp = "2024-01-03T00:04:00Z",
                        volume = 650,
                        vwap = 185.26
                    ), StockBar(
                        close = 185.24,
                        high = 185.24,
                        low = 185.24,
                        trades = 40,
                        open = 185.24,
                        timestamp = "2024-01-03T00:06:00Z",
                        volume = 982,
                        vwap = 185.24
                    ), StockBar(
                        close = 185.24,
                        high = 185.24,
                        low = 185.24,
                        trades = 30,
                        open = 185.24,
                        timestamp = "2024-01-03T00:07:00Z",
                        volume = 2718,
                        vwap = 185.24
                    )
                )
            ), nextPageToken = null
        )
        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockStockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }

        val stockAggregationRequest = defaultStockAggregationRequest.copy()
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
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(stockAggregationRequest)
        }

        val response = httpResponse.body<StockAggregationResponse>()
        assertEquals(OK, httpResponse.status)
        assertNotEquals(emptyMap(), response.bars)

        unloadKoinModules(overrides)
    }

    @Test
    fun `Get Historical Bars Request with bad parameter`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockEngine = MockEngine { _ ->
            respond(
                content = mapOf("message" to "Invalid format for parameter symbols: query parameter 'symbols' is required").toString(),
                status = BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }

        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        stockAggregationRequest.symbols = ""
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
        val httpResponse = client.post("/HistoricalBars/Request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(stockAggregationRequest)
        }
        assertEquals(BadRequest, httpResponse.status)

        unloadKoinModules(overrides)
    }

    @Test
    fun `Get opening hours`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockClockResponse = OpeningHours(
            isOpen = false,
            nextClose = "2025-11-04T16:00:00-05:00",
            nextOpen = "2025-11-04T09:30:00-05:00",
            timestamp = "2025-11-03T16:48:13.091852201-05:00"
        )

        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockClockResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }
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
        val httpResponse = client.get("/clock") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        assertEquals(OK, httpResponse.status)
        val openingHours = httpResponse.body<OpeningHours>()
        assertEquals(false, openingHours.isOpen)
        assertEquals("2025-11-04T16:00:00-05:00", openingHours.nextClose)
        assertEquals("2025-11-04T09:30:00-05:00", openingHours.nextOpen)
        assertEquals("2025-11-03T16:48:13.091852201-05:00", openingHours.timestamp)
        unloadKoinModules(overrides)
    }

    @Test
    fun `Create Good Order with limit type from Alpaca`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }

        val mockOrderResponse = OrderResponse(
            id = "64c48451-34c4-4642-83a8-43f5f75bf6fd",
            clientOrderId = "2b3b5479-923b-4b5e-a95c-00e0b8d0280a",
            createdAt = "2025-03-23T20:09:23.265233813Z",
            updatedAt = "2025-03-23T20:09:23.266033472Z",
            submittedAt = "2025-03-23T20:09:23.265233813Z",
            filledAt = null,
            expiredAt = null,
            canceledAt = null,
            failedAt = null,
            replacedAt = null,
            replacedBy = null,
            replaces = null,
            assetId = "b0b6dd9d-8b9b-48a9-ba46-b9d54906e415",
            symbol = "AAPL",
            assetClass = "us_equity",
            notional = "150",
            filledQty = "0",
            filledAvgPrice = null,
            orderClass = "",
            type = "limit",
            side = "buy",
            positionIntent = "buy_to_open",
            timeInForce = "day",
            limitPrice = "140",
            stopPrice = null,
            status = "accepted",
            extendedHours = false,
            legs = null,
            trailPercent = null,
            trailPrice = null,
            hwm = null,
            subtag = null,
            source = null,
            expiresAt = "2025-03-24T20:00:00Z"
        )
        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockOrderResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val overrides = module { single<HttpClientEngine> { mockEngine } }
        application { loadKoinModules(overrides) }

        val orderRequest = OrderRequest(
            symbol = "AAPL",
            notional = "150",
            side = "buy",
            type = "limit",
            limitPrice = "140",
            timeInForce = "day",
            extendedHours = false
        )
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
        val httpResponse = client.post("/Order/Create") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(orderRequest)
        }

        assertEquals(OK, httpResponse.status)
        val response = httpResponse.body<OrderResponse>()
        assertEquals("64c48451-34c4-4642-83a8-43f5f75bf6fd", response.id)
        assertEquals("150", response.notional)
        assertEquals("limit", response.type)
        assertEquals("day", response.timeInForce)
        assertEquals("140", response.limitPrice)
        assertEquals("b0b6dd9d-8b9b-48a9-ba46-b9d54906e415", response.assetId)
    }
}