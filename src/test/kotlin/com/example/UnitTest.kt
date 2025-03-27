package com.example


import com.example.data.singleModels.*
import com.example.tradingLogic.BacktestConfig
import com.example.tradingLogic.BacktestResult
import com.example.tradingLogic.strategies.Strategies
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UnitTest {

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


        private fun getMockAlpacaClient(mockEngine: MockEngine) = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = false
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
            install(DefaultRequest) {
                header("APCA-API-KEY-ID", com.example.data.AlpacaRepository.PAPERAPIKEY)
                header("APCA-API-SECRET-KEY", com.example.data.AlpacaRepository.PAPERSECRET)
                header("content-type", "application/json")
                header("accept", "application/json")
            }
        }
    }

    // Used to connect to in-memory server
    private fun getClient(engine : HttpClientEngine): HttpClient = HttpClient(engine) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
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

    @Test
    fun `Create a Good Order to Alpaca`() = testApplication {

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
                status = HttpStatusCode.OK,
            )
        }

        application {
            configureRouting()
        }

        // Precondition
        val orderRequest = defaultOrderRequest.copy()

//        val httpResponse = getClient().post("/Order/Create") {
//            setBody(orderRequest)
//        }
//        val response = httpResponse.body<OrderResponse>()
//        assertEquals(HttpStatusCode.OK, httpResponse.status)
//        assertEquals(orderRequest.quantity, response.qty)
//        assertEquals(orderRequest.symbol, response.symbol)

    }

    @Test
    fun `Create a Bad Order to Alpaca`() = testApplication {

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
                status = HttpStatusCode.UnprocessableEntity,
            )
        }
        application {
           // modules(testModule)
        }

        // Precondition
        val orderRequest = defaultOrderRequest.copy()
        orderRequest.symbol = ""

//        val httpResponse = getClient().post("/Order/Create") {
//            setBody(orderRequest)
//        }
//
//        val response = httpResponse.bodyAsText()
//        assertEquals(HttpStatusCode.UnprocessableEntity, httpResponse.status)

    }

    @Test
    fun `Get Account call to Alpaca`() = testApplication {

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
                status = HttpStatusCode.OK,
            )
        }

        application {
            configureRouting()
        }

//        val accountId = "PA3ALX4NGLN0"
//        val state = "ACTIVE"
//        val client = getClient()
//        val httpResponse = client.get("/AccountDetails")
//
//        assertEquals(HttpStatusCode.OK, httpResponse.status)
//        val accountDetails = httpResponse.body<Account>()
//        assertEquals(accountId, accountDetails.accountNumber)
//        assertEquals(state, accountDetails.status)
    }

    @Test
    fun `Get Historical Bars Request with good parameter`() = testApplication {

        val mockStockAggregationResponse = StockAggregationResponse(
            bars = mapOf(
                "AAPL" to listOf(
                    StockBar(close = 185.31, high = 185.31, low = 185.31, trades = 28, open = 185.31, timestamp = "2024-01-03T00:00:00Z", volume = 1045, vwap = 185.31),
                    StockBar(close = 185.29, high = 185.29, low = 185.29, trades = 36, open = 185.29, timestamp = "2024-01-03T00:01:00Z", volume = 283, vwap = 185.29),
                    StockBar(close = 185.29, high = 185.29, low = 185.29, trades = 26, open = 185.29, timestamp = "2024-01-03T00:02:00Z", volume = 381, vwap = 185.29),
                    StockBar(close = 185.26, high = 185.26, low = 185.26, trades = 30, open = 185.26, timestamp = "2024-01-03T00:04:00Z", volume = 650, vwap = 185.26),
                    StockBar(close = 185.24, high = 185.24, low = 185.24, trades = 40, open = 185.24, timestamp = "2024-01-03T00:06:00Z", volume = 982, vwap = 185.24),
                    StockBar(close = 185.24, high = 185.24, low = 185.24, trades = 30, open = 185.24, timestamp = "2024-01-03T00:07:00Z", volume = 2718, vwap = 185.24)
                )
            ),
            nextPageToken = null
        )
        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockStockAggregationResponse),
                status = HttpStatusCode.OK,
            )
        }

        application {
            install(Koin){
                modules (org.koin.dsl.module {
                        single { getClient(mockEngine) }
                    }
                )
            }
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()

        val httpResponse = restClient().post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }

        val response = httpResponse.body<StockAggregationResponse>()
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        assertNotEquals(emptyMap(), response.bars)
    }

    @Test
    fun `Get Historical Bars Request with bad parameter`() = testApplication {
        val mockEngine = MockEngine { _ ->
            respond(
                content =  mapOf("message" to "Invalid format for parameter symbols: query parameter 'symbols' is required").toString(),
                status = HttpStatusCode.BadRequest
            )
        }
        application {
            configureRouting()
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        stockAggregationRequest.symbols = ""

        val httpResponse = getClient().post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }
}
