package com.example


import com.example.data.AlpacaRepository
import com.example.data.TradingRepository
import com.example.data.singleModels.*
import com.example.di.appModule
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Op
import org.koin.core.component.inject
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
            single { TradingController() }
        }
    }

    @BeforeTest
    fun setup() {
        startKoin{
            modules(testModule)
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
                modules(org.koin.dsl.module {
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
                modules(org.koin.dsl.module {
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
                modules(org.koin.dsl.module {
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
                modules(org.koin.dsl.module {
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
                modules(module {
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
    fun `Strategy Execution Algorithm`() = testApplication {
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
    fun `Backtesting without API Access`() = testApplication {

        val tradingBot by inject<TradingBot>()

        val strategy = Strategies.MovingAverage
        val stockAggregationRequest = StockAggregationRequest()
        when (val result = tradingBot.backtest(strategy, stockAggregationRequest)) {
            is Result.Error<*, *>   -> {
                when(result.error) {
                    TradingLogicError.DataError.NO_SUFFICIENT_ACCOUNT_BALANCE      -> TODO()
                    TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE       -> TODO()
                    TradingLogicError.DataError.HISTORICAL_DATA_TOO_MANY_REQUESTS  -> TODO()
                    TradingLogicError.DataError.INVALID_PARAMETER_FORMAT           -> TODO()
                    TradingLogicError.DataError.LIST_IS_EMPTY                      -> TODO()
                    TradingLogicError.DataError.UNAUTHORIZED                       -> TODO()
                    TradingLogicError.DataError.MISC_ERROR                         -> TODO()
                    TradingLogicError.RunError.ALREADY_RUNNING                     -> TODO()
                    TradingLogicError.RunError.TIME_FRAME_COULD_NOT_PARSED         -> TODO()
                    TradingLogicError.StrategyError.NO_STRATEGY_SELECTED           -> TODO()
                    TradingLogicError.StrategyError.WRONG_SYMBOLS_PROVIDED         -> TODO()
                    TradingLogicError.StrategyError.NO_SYMBOLS_PROVIDED            -> TODO()
                }
            }
            is Result.Success<*, *> -> assertTrue(true)
        }
    }
}