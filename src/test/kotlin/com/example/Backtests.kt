package com.example


import com.example.backtestdata.JsonToDataClassConverter
import com.example.services.TraderService
import com.example.data.alpaca.AlpacaRepository
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.singleModels.StockAggregationResponse
import com.example.data.singleModels.StockBar
import com.example.tradinglogic.*
import com.example.tradinglogic.strategies.Strategies
import com.example.tradinglogic.strategies.StrategyFactory
import com.example.tradinglogic.strategies.TradingSignal
import com.example.tradinglogic.strategies.TradingStrategy
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*

class Backtest : KoinTest {

    companion object {

        val defaultStockAggregationRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "1Min",
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

        val testModule = module {
            single<AlpacaRepository> { AlpacaRepository() }
            single<TraderService> { TraderService() }
            single { TradingBot() }
        }
    }

    @After
    fun tearDown() {
        stopKoin()
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
    fun `Backtesting with too less historical bars`() {
        val mockStockAggregationResponse = StockAggregationResponse(
            bars = mapOf(
                "AAPL" to listOf(
                    StockBar(
                        close = 150.25,
                        high = 151.00,
                        low = 149.80,
                        trades = 40,
                        open = 150.80,
                        timestamp = "2024-02-05T09:30:00Z",
                        volume = 2000,
                        vwap = 150.50
                    ),
                    StockBar(
                        close = 162.80,
                        high = 163.20,
                        low = 160.50,
                        trades = 55,
                        open = 151.00,
                        timestamp = "2024-02-05T09:31:00Z",
                        volume = 2500,
                        vwap = 162.00
                    ),
                    StockBar(
                        close = 140.10,
                        high = 142.00,
                        low = 138.00,
                        trades = 60,
                        open = 162.70,
                        timestamp = "2024-02-05T09:32:00Z",
                        volume = 3000,
                        vwap = 140.90
                    ),
                    StockBar(
                        close = 175.60,
                        high = 176.00,
                        low = 172.50,
                        trades = 70,
                        open = 140.20,
                        timestamp = "2024-02-05T09:33:00Z",
                        volume = 2800,
                        vwap = 174.00
                    ),
                    StockBar(
                        close = 132.40,
                        high = 135.00,
                        low = 130.00,
                        trades = 65,
                        open = 175.50,
                        timestamp = "2024-02-05T09:34:00Z",
                        volume = 3200,
                        vwap = 133.20
                    ),
                    StockBar(
                        close = 180.90,
                        high = 182.00,
                        low = 179.00,
                        trades = 75,
                        open = 132.50,
                        timestamp = "2024-02-05T09:35:00Z",
                        volume = 3500,
                        vwap = 181.20
                    )
                )
            ),
            nextPageToken = null
        )

        val mockEngine = MockEngine { request ->
            println("🔧 MockEngine intercepted request to: ${request.url}")
            respond(
                content = Json.encodeToString(mockStockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        startKoin {
            modules(testModule, module {
                single<HttpClientEngine> { mockEngine }
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }

        val backtestConfig = BacktestConfig(
            stockAggregationRequest = defaultStockAggregationRequest.copy(),
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()
        val resultWithLessData = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        when (resultWithLessData) {
            is Result.Success<*, *> -> fail("resultValue could not be casted $resultWithLessData")
            is Result.Error<*, *> -> assertEquals(
                TradingLogicError.DataError.TOO_LESS_DATA_SAMPLES,
                resultWithLessData.error
            )
        }
        stopKoin()
    }

    @Test
    fun `Backtesting with default stockAggregation AAPL`() {

        val stockAggregationResponse: StockAggregationResponse =
            JsonToDataClassConverter.stockAggregationResponseFromResource("AAPL_1.json")

        val mockEngine = MockEngine { request ->
            println("🔧 MockEngine intercepted request to: ${request.url}")
            respond(
                content = Json.encodeToString(stockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        startKoin {
            modules(testModule, module {
                single<HttpClientEngine> { mockEngine }
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }
        val backtestConfig = BacktestConfig(
            stockAggregationRequest = defaultStockAggregationRequest.copy(),
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()

        val resultWithDefault = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        val defaultBackTestResult = BacktestResult()
        when (resultWithDefault) {
            is Result.Success<*, *> -> {
                val resultValue = resultWithDefault.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(defaultBackTestResult.finalEquity, resultValue.finalEquity)
                    assertNotEquals(defaultBackTestResult.roiPercent, resultValue.roiPercent)
                    assertNotEquals(defaultBackTestResult.winRatePercent, resultValue.winRatePercent)
                    assertNotEquals(defaultBackTestResult.positions, resultValue.positions)
                } else {
                    fail("resultValue could not be casted")
                }
            }
            is Result.Error<*, *> -> fail("Expected success but got Error: ${resultWithDefault.error}")
        }
        stopKoin()
    }

    @Test
    fun `Backtesting with default stockAggregation TSLA`() {

        val stockAggregationResponse: StockAggregationResponse =
            JsonToDataClassConverter.stockAggregationResponseFromResource("TSLA_1.json")

        val mockEngine = MockEngine { request ->
            println("🔧 MockEngine intercepted request to: ${request.url}")
            respond(
                content = Json.encodeToString(stockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        startKoin {
            modules(testModule, module {
                single<HttpClientEngine> { mockEngine }
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }

        val stockAggregationRequest = defaultStockAggregationRequest.copy(
            symbols = "TSLA"
        )
        val backtestConfig = BacktestConfig(
            stockAggregationRequest = stockAggregationRequest,
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()

        val result = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        val backTestResult = BacktestResult()
        when (result) {
            is Result.Success<*, *> -> {
                val resultValue = result.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(backTestResult.finalEquity, resultValue.finalEquity)
                    assertNotEquals(backTestResult.roiPercent, resultValue.roiPercent)
                    assertNotEquals(backTestResult.winRatePercent, resultValue.winRatePercent)
                    assertNotEquals(backTestResult.positions, resultValue.positions)
                } else {
                    fail("resultValue could not be casted")
                }
            }

            is Result.Error<*, *> -> fail("Expected success but got Error: ${result.error}")
        }
        stopKoin()
    }


    @Test
    fun `Backtesting with default stockAggregation TSLA with 3years`() {

        val stockAggregationResponse: StockAggregationResponse =
            JsonToDataClassConverter.stockAggregationResponseFromResource("TSLA_2.json")

        val mockEngine = MockEngine { request ->
            println("🔧 MockEngine intercepted request to: ${request.url}")
            respond(
                content = Json.encodeToString(stockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        startKoin {
            modules(testModule, module {
                single<HttpClientEngine> { mockEngine }
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }

        val stockAggregationRequest = defaultStockAggregationRequest.copy(
            symbols = "TSLA"
        )
        val backtestConfig = BacktestConfig(
            stockAggregationRequest = stockAggregationRequest,
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()

        val result = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        val backTestResult = BacktestResult()
        when (result) {
            is Result.Success<*, *> -> {
                val resultValue = result.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(backTestResult.finalEquity, resultValue.finalEquity)
                    assertNotEquals(backTestResult.roiPercent, resultValue.roiPercent)
                    assertNotEquals(backTestResult.winRatePercent, resultValue.winRatePercent)
                    assertNotEquals(backTestResult.positions, resultValue.positions)
                } else {
                    fail("resultValue could not be casted")
                }
            }

            is Result.Error<*, *> -> fail("Expected success but got Error: ${result.error}")
        }
        stopKoin()
    }

    @Test
    fun `Backtesting with default stockAggregation GOOGL`() {

        val stockAggregationResponse: StockAggregationResponse =
            JsonToDataClassConverter.stockAggregationResponseFromResource("GOOGL_1.json")

        val mockEngine = MockEngine { request ->
            println("🔧 MockEngine intercepted request to: ${request.url}")
            respond(
                content = Json.encodeToString(stockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        startKoin {
            modules(testModule, module {
                single<HttpClientEngine> { mockEngine }
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }
        val stockAggregationRequest = defaultStockAggregationRequest.copy(
            symbols = "GOOGL",
            timeframe = "30Min",
            startDateTime = "2025-12-29T00:00:00Z",
            endDateTime = "2026-01-22T00:00:00Z",
            limit = 10000
        )

        val backtestConfig = BacktestConfig(
            stockAggregationRequest = stockAggregationRequest,
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()

        val result = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        val defaultBackTestResult = BacktestResult()
        when (result) {
            is Result.Success<*, *> -> {
                val resultValue = result.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(defaultBackTestResult.finalEquity, resultValue.finalEquity)
                    assertNotEquals(defaultBackTestResult.roiPercent, resultValue.roiPercent)
                    assertNotEquals(defaultBackTestResult.winRatePercent, resultValue.winRatePercent)
                    assertNotEquals(defaultBackTestResult.positions, resultValue.positions)
                } else {
                    fail("resultValue could not be casted")
                }
            }
            is Result.Error<*, *> -> fail("Expected success but got Error: ${result.error}")
        }
        stopKoin()
    }

    @Test
    fun `Backtesting with default stockAggregation and fake data`() {

        val stockAggregationResponse: StockAggregationResponse =
            JsonToDataClassConverter.stockAggregationResponseFromResource("AAPL_fake.json")

        val mockEngine = MockEngine { request ->
            println("🔧 MockEngine intercepted request to: ${request.url}")
            respond(
                content = Json.encodeToString(stockAggregationResponse),
                status = OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        startKoin {
            modules(testModule, module {
                single<HttpClientEngine> { mockEngine }
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }
        val backtestConfig = BacktestConfig(
            stockAggregationRequest = defaultStockAggregationRequest.copy(),
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()

        val resultWithDefault = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        val defaultBackTestResult = BacktestResult()
        when (resultWithDefault) {
            is Result.Success<*, *> -> {
                val resultValue = resultWithDefault.data
                if (resultValue is BacktestResult) {
                    assertEquals(Strategies.MovingAverage, resultValue.strategyName)
                    assertNotEquals(defaultBackTestResult.finalEquity, resultValue.finalEquity)
                    assertNotEquals(defaultBackTestResult.roiPercent, resultValue.roiPercent)
                } else {
                    fail("resultValue could not be casted")
                }
            }
            is Result.Error<*, *> -> fail("Expected success but got Error: ${resultWithDefault.error}")
        }
        stopKoin()
    }

    @Test
    fun `Backtesting without valid Indicators`() {
        startKoin {
            modules(testModule, module {
                single<HttpClient> {
                    HttpClient(get()) {
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = false
                                ignoreUnknownKeys = true
                                encodeDefaults = true
                            })
                        }
                        install(Logging) {
                            logger = Logger.DEFAULT
                            level = LogLevel.ALL
                        }
                        install(DefaultRequest) {
                            header("content-type", "application/json")
                            header("accept", "application/json")
                        }
                    }
                }
            })
        }
        val stockAggregationRequest = defaultStockAggregationRequest.copy(symbols = "")
        val backtestConfig = BacktestConfig(
            stockAggregationRequest = stockAggregationRequest,
            strategySelector = Strategies.MovingAverage
        )
        val tradingBot by inject<TradingBot>()
        val resultWithEmptySymbol = runBlocking {
            tradingBot.backtest(backtestConfig)
        }
        when (resultWithEmptySymbol) {
            is Result.Success<*, *> -> fail("Expected Success but got Error: ${resultWithEmptySymbol.data}")
            is Result.Error<*, *> -> assertEquals(
                TradingLogicError.StrategyError.NO_SYMBOLS_PROVIDED,
                resultWithEmptySymbol.error
            )
        }
        stopKoin()
    }
}
