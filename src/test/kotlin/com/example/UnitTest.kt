package com.example


import com.example.data.AlpacaRepository
import com.example.data.TradingRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.*
import com.example.tradingLogic.strategies.Strategies
import com.example.tradingLogic.strategies.StrategyFactory
import com.example.tradingLogic.strategies.TradingSignal
import com.example.tradingLogic.strategies.TradingStrategy
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*

class UnitTest : KoinTest {

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
            install(ContentNegotiation) {
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
                header("APCA-API-KEY-ID", AlpacaRepository.PAPERAPIKEY)
                header("APCA-API-SECRET-KEY", AlpacaRepository.PAPERSECRET)
                header("content-type", "application/json")
                header("accept", "application/json")
            }
        }
        val testModule = module {
            single { CIO.create() }
            single<TradingRepository> { AlpacaRepository() }
            single { TradingBot() }
        }
    }
    @AfterTest
    fun tearDown() {
        stopKoin()
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
                status = OK,
            )
        }
        application {
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }

        // Precondition
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
        val httpResponse = client.post("/Order/Create") {
            setBody(orderRequest)
        }
        val response = httpResponse.body<OrderResponse>()
        assertEquals(OK, httpResponse.status)
        assertEquals(orderRequest.quantity, response.qty)
        assertEquals(orderRequest.symbol, response.symbol)
        assertEquals(mockOrderResponse.symbol, response.symbol)
        assertEquals(mockOrderResponse.qty, response.qty)
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
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }

        // Precondition
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
        val httpResponse = client.post("/Order/Create") {
            setBody(orderRequest)
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, httpResponse.status)
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
                status = OK,
            )
        }

        application {
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }

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
        val httpResponse = client.get("/AccountDetails")

        assertEquals(OK, httpResponse.status)
        val accountDetails = httpResponse.body<Account>()
        assertEquals(accountId, accountDetails.accountNumber)
        assertEquals(state, accountDetails.status)
    }

    @Test
    fun `Get Historical Bars Request with good parameter`() = testApplication {

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
                    ),
                    StockBar(
                        close = 185.29,
                        high = 185.29,
                        low = 185.29,
                        trades = 36,
                        open = 185.29,
                        timestamp = "2024-01-03T00:01:00Z",
                        volume = 283,
                        vwap = 185.29
                    ),
                    StockBar(
                        close = 185.29,
                        high = 185.29,
                        low = 185.29,
                        trades = 26,
                        open = 185.29,
                        timestamp = "2024-01-03T00:02:00Z",
                        volume = 381,
                        vwap = 185.29
                    ),
                    StockBar(
                        close = 185.26,
                        high = 185.26,
                        low = 185.26,
                        trades = 30,
                        open = 185.26,
                        timestamp = "2024-01-03T00:04:00Z",
                        volume = 650,
                        vwap = 185.26
                    ),
                    StockBar(
                        close = 185.24,
                        high = 185.24,
                        low = 185.24,
                        trades = 40,
                        open = 185.24,
                        timestamp = "2024-01-03T00:06:00Z",
                        volume = 982,
                        vwap = 185.24
                    ),
                    StockBar(
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
            ),
            nextPageToken = null
        )
        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockStockAggregationResponse),
                status = OK,
            )
        }

        application {
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }
        // Preconditions
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

        val httpResponse = client.post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }

        val response = httpResponse.body<StockAggregationResponse>()
        assertEquals(OK, httpResponse.status)
        assertNotEquals(emptyMap(), response.bars)
    }

    @Test
    fun `Get Historical Bars Request with bad parameter`() = testApplication {
        val mockEngine = MockEngine { _ ->
            respond(
                content = mapOf("message" to "Invalid format for parameter symbols: query parameter 'symbols' is required").toString(),
                status = BadRequest
            )
        }
        application {
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        stockAggregationRequest.symbols = ""
        val mockClient = createClient {
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
        val httpResponse = mockClient.post("/HistoricalBars/Request") {
            setBody(stockAggregationRequest)
        }
        assertEquals(BadRequest, httpResponse.status)
    }

    @Test
    fun `Strategy Execution Algorithm`() {
        val strategy: TradingStrategy = StrategyFactory().createStrategy(Strategies.MovingAverage)
        val indicatorSnapshot = IndicatorSnapshot(
            originalPrice = 101.5,
            resistance = 105.0,
            support = 98.0,
            averageBollingerBand = 100.0,
            lowerBollingerBand = 95.0,
            upperBollingerBand = 105.0,
            shortSMA = 102.0,
            longSMA = 99.5,
            rsi = 55.0
        )
        val tradingSignal = strategy.executeAlgorithm(indicatorSnapshot)
        assertEquals(TradingSignal.Buy, tradingSignal)

        val indicatorSnapshot2 = IndicatorSnapshot(
            originalPrice = 101.5,
            resistance = 105.0,
            support = 98.0,
            averageBollingerBand = 100.0,
            lowerBollingerBand = 95.0,
            upperBollingerBand = 105.0,
            shortSMA = 102.0,
            longSMA = 103.5,
            rsi = 55.0
        )
        val tradingSignal2 = strategy.executeAlgorithm(indicatorSnapshot2)
        assertEquals(TradingSignal.Sell, tradingSignal2)

        val indicatorSnapshot3 = IndicatorSnapshot(
            originalPrice = 101.5,
            resistance = 105.0,
            support = 98.0,
            averageBollingerBand = 100.0,
            lowerBollingerBand = 95.0,
            upperBollingerBand = 105.0,
            shortSMA = 104.5,
            longSMA = 104.5,
            rsi = 55.0
        )
        val tradingSignal3 = strategy.executeAlgorithm(indicatorSnapshot3)
        assertEquals(TradingSignal.Hold, tradingSignal3)

        val indicatorSnapshot4 = IndicatorSnapshot(
            originalPrice = 101.5,
            resistance = 105.0,
            support = 98.0,
            averageBollingerBand = 100.0,
            lowerBollingerBand = 95.0,
            upperBollingerBand = 105.0,
            shortSMA = 2373.54,
            longSMA = 2373.54,
            rsi = 55.0
        )
        val tradingSignal4 = strategy.executeAlgorithm(indicatorSnapshot4)
        assertEquals(TradingSignal.Hold, tradingSignal4)
    }

    @Test
    fun `Backtesting with default stockAggregation, bad history`() = testApplication {
        val mockStockAggregationResponse = StockAggregationResponse(
            bars = mapOf(
                "AAPL" to listOf(
                    StockBar(close = 150.25, high = 151.00, low = 149.80, trades = 40, open = 150.80, timestamp = "2024-02-05T09:30:00Z", volume = 2000, vwap = 150.50),
                    StockBar(close = 162.80, high = 163.20, low = 160.50, trades = 55, open = 151.00, timestamp = "2024-02-05T09:31:00Z", volume = 2500, vwap = 162.00),
                    StockBar(close = 140.10, high = 142.00, low = 138.00, trades = 60, open = 162.70, timestamp = "2024-02-05T09:32:00Z", volume = 3000, vwap = 140.90),
                    StockBar(close = 175.60, high = 176.00, low = 172.50, trades = 70, open = 140.20, timestamp = "2024-02-05T09:33:00Z", volume = 2800, vwap = 174.00),
                    StockBar(close = 132.40, high = 135.00, low = 130.00, trades = 65, open = 175.50, timestamp = "2024-02-05T09:34:00Z", volume = 3200, vwap = 133.20),
                    StockBar(close = 180.90, high = 182.00, low = 179.00, trades = 75, open = 132.50, timestamp = "2024-02-05T09:35:00Z", volume = 3500, vwap = 181.20),
                    StockBar(close = 125.30, high = 128.00, low = 124.50, trades = 68, open = 180.50, timestamp = "2024-02-05T09:36:00Z", volume = 3100, vwap = 126.70),
                    StockBar(close = 190.15, high = 191.00, low = 188.00, trades = 80, open = 125.80, timestamp = "2024-02-05T09:37:00Z", volume = 4000, vwap = 189.20)
                )
            ),
            nextPageToken = null
        )

        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockStockAggregationResponse),
                status = OK,
            )
        }

        application {
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }
        startKoin {
            modules(testModule)
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        val tradingBot by inject<TradingBot>()

        val resultWithDefault = runBlocking {
            tradingBot.backtest(Strategies.MovingAverage, stockAggregationRequest)
        }
        val defaultBackTestResult = BacktestResult()
        when (resultWithDefault) {
            is Result.Success<*, *> -> {
                val resultValue = resultWithDefault.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(defaultBackTestResult.finalBalance, resultValue.finalBalance)
                    assertNotEquals(defaultBackTestResult.winRate, resultValue.winRate)
                    assertNotEquals(defaultBackTestResult.positions, resultValue.positions)
                    println("Strategy: ${resultValue.strategyName}")
                    println("Final Balance: ${resultValue.finalBalance}")
                    println("Win Rate: ${resultValue.winRate}")
                    println("Positions: ${resultValue.positions}")
                } else {
                    fail("resultValue could not be casted")
                }
            }
            is Result.Error<*, *>   -> fail("Expected success but got Error: ${resultWithDefault.error}")
        }
        stopKoin()
    }

    @Test
    fun `Backtesting with default stockAggregation`() = testApplication {
        val mockStockAggregationResponse = StockAggregationResponse(
            bars = mapOf(
                "AAPL" to listOf(
                    StockBar(close = 185.31, high = 185.31, low = 185.31, trades = 28, open = 185.31, timestamp = "2024-01-03T00:00:00Z", volume = 1045, vwap = 185.31),
                    StockBar(close = 185.29, high = 185.29, low = 185.29, trades = 36, open = 185.29, timestamp = "2024-01-03T00:01:00Z", volume = 283, vwap = 185.29),
                    StockBar(close = 185.29, high = 185.29, low = 185.29, trades = 26, open = 185.29, timestamp = "2024-01-03T00:02:00Z", volume = 381, vwap = 185.29),
                    StockBar(close = 185.26, high = 185.26, low = 185.26, trades = 30, open = 185.26, timestamp = "2024-01-03T00:04:00Z", volume = 650, vwap = 185.26),
                    StockBar(close = 185.24, high = 185.24, low = 185.24, trades = 40, open = 185.24, timestamp = "2024-01-03T00:06:00Z", volume = 982, vwap = 185.24),
                    StockBar(close = 185.24, high = 185.24, low = 185.24, trades = 30, open = 185.24, timestamp = "2024-01-03T00:07:00Z", volume = 2718, vwap = 185.24),
                    StockBar(close = 185.30, high = 185.35, low = 185.25, trades = 50, open = 185.27, timestamp = "2024-01-03T00:08:00Z", volume = 1200, vwap = 185.29),
                    StockBar(close = 185.32, high = 185.38, low = 185.28, trades = 45, open = 185.30, timestamp = "2024-01-03T00:09:00Z", volume = 1100, vwap = 185.32),
                    StockBar(close = 185.33, high = 185.40, low = 185.30, trades = 42, open = 185.31, timestamp = "2024-01-03T00:10:00Z", volume = 1300, vwap = 185.34),
                    StockBar(close = 185.35, high = 185.45, low = 185.33, trades = 48, open = 185.32, timestamp = "2024-01-03T00:11:00Z", volume = 1400, vwap = 185.36),
                    StockBar(close = 185.34, high = 185.46, low = 185.31, trades = 39, open = 185.33, timestamp = "2024-01-03T00:12:00Z", volume = 1250, vwap = 185.35),
                    StockBar(close = 185.36, high = 185.48, low = 185.34, trades = 55, open = 185.34, timestamp = "2024-01-03T00:13:00Z", volume = 1600, vwap = 185.38),
                    StockBar(close = 185.37, high = 185.50, low = 185.36, trades = 60, open = 185.35, timestamp = "2024-01-03T00:14:00Z", volume = 1700, vwap = 185.39),
                    StockBar(close = 185.39, high = 185.52, low = 185.37, trades = 62, open = 185.37, timestamp = "2024-01-03T00:15:00Z", volume = 1800, vwap = 185.41),
                    StockBar(close = 185.38, high = 185.51, low = 185.36, trades = 57, open = 185.38, timestamp = "2024-01-03T00:16:00Z", volume = 1500, vwap = 185.40),
                    StockBar(close = 185.40, high = 185.53, low = 185.39, trades = 58, open = 185.38, timestamp = "2024-01-03T00:17:00Z", volume = 1550, vwap = 185.42),
                    StockBar(close = 185.41, high = 185.55, low = 185.40, trades = 54, open = 185.40, timestamp = "2024-01-03T00:18:00Z", volume = 1650, vwap = 185.43),
                    StockBar(close = 185.43, high = 185.57, low = 185.41, trades = 53, open = 185.42, timestamp = "2024-01-03T00:19:00Z", volume = 1750, vwap = 185.45),
                    StockBar(close = 185.42, high = 185.56, low = 185.40, trades = 59, open = 185.43, timestamp = "2024-01-03T00:20:00Z", volume = 1600, vwap = 185.44),
                    StockBar(close = 185.44, high = 185.58, low = 185.42, trades = 61, open = 185.44, timestamp = "2024-01-03T00:21:00Z", volume = 1700, vwap = 185.46),
                    StockBar(close = 185.45, high = 185.60, low = 185.44, trades = 63, open = 185.45, timestamp = "2024-01-03T00:22:00Z", volume = 1800, vwap = 185.47),
                    StockBar(close = 185.47, high = 185.62, low = 185.45, trades = 65, open = 185.46, timestamp = "2024-01-03T00:23:00Z", volume = 1900, vwap = 185.49),
                    StockBar(close = 185.46, high = 185.61, low = 185.44, trades = 64, open = 185.47, timestamp = "2024-01-03T00:24:00Z", volume = 1850, vwap = 185.48),
                    StockBar(close = 185.48, high = 185.63, low = 185.46, trades = 66, open = 185.48, timestamp = "2024-01-03T00:25:00Z", volume = 1950, vwap = 185.50),
                    StockBar(close = 185.49, high = 185.65, low = 185.47, trades = 68, open = 185.49, timestamp = "2024-01-03T00:26:00Z", volume = 2000, vwap = 185.51),
                    StockBar(close = 185.50, high = 185.66, low = 185.48, trades = 70, open = 185.50, timestamp = "2024-01-03T00:27:00Z", volume = 2100, vwap = 185.52)
                )
            ),
            nextPageToken = null
        )

        val mockEngine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(mockStockAggregationResponse),
                status = OK,
            )
        }

        application {
            install(Koin) {
                modules(testModule, module {
                    single<HttpClientEngine> { mockEngine }
                })
            }
            configureSerialization()
            configureRouting()
        }
        startKoin {
            modules(testModule)
        }
        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        val tradingBot by inject<TradingBot>()

        val resultWithDefault = runBlocking {
            tradingBot.backtest(Strategies.MovingAverage, stockAggregationRequest)
        }
        val defaultBackTestResult = BacktestResult()
        when (resultWithDefault) {
            is Result.Success<*, *> -> {
                val resultValue = resultWithDefault.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(defaultBackTestResult.finalBalance, resultValue.finalBalance)
                    assertNotEquals(defaultBackTestResult.winRate, resultValue.winRate)
                    assertNotEquals(defaultBackTestResult.positions, resultValue.positions)
                    println("Strategy: ${resultValue.strategyName}")
                    println("Final Balance: ${resultValue.finalBalance}")
                    println("Win Rate: ${resultValue.winRate}")
                    println("Positions: ${resultValue.positions}")
                } else {
                    fail("resultValue could not be casted")
                }
            }
            is Result.Error<*, *>   -> fail("Expected success but got Error: ${resultWithDefault.error}")
        }
        stopKoin()
    }

    @Test
    fun `Backtesting without valid Indicators`()  {
        startKoin {
            modules(testModule)
        }

        val request = StockAggregationRequest()
        request.symbols = ""
        val tradingBot by inject<TradingBot>()
        val resultWithEmptySymbol = runBlocking {
            tradingBot.backtest(Strategies.MovingAverage, request)
        }
        when (resultWithEmptySymbol) {
            is Result.Success<*, *> -> fail("Expected Success but got Error: ${resultWithEmptySymbol.data}")
            is Result.Error<*, *>   -> assertEquals(TradingLogicError.StrategyError.NO_SYMBOLS_PROVIDED, resultWithEmptySymbol.error)
        }
        stopKoin()
    }

    @Test
    fun `Backtesting no Strategy Selected`() {
        startKoin {
            modules(testModule)
        }
        val request = StockAggregationRequest()
        val tradingBot by inject<TradingBot>()

        val resultNoStrategy = runBlocking {
            tradingBot.backtest(Strategies.None, request)
        }

        when (resultNoStrategy) {
            is Result.Success<*, *> -> fail("Expected Success but got Error: ${resultNoStrategy.data}")
            is Result.Error<*, *>   -> assertEquals(TradingLogicError.StrategyError.NO_STRATEGY_SELECTED, resultNoStrategy.error)
        }
        stopKoin()
    }
}