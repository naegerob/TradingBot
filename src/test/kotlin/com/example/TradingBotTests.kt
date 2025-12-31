package com.example


import com.example.data.TraderService
import com.example.data.alpaca.AlpacaRepository
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.singleModels.StockAggregationResponse
import com.example.data.singleModels.StockBar
import com.example.tradingLogic.*
import com.example.tradingLogic.strategies.Strategies
import com.example.tradingLogic.strategies.StrategyFactory
import com.example.tradingLogic.strategies.TradingSignal
import com.example.tradingLogic.strategies.TradingStrategy
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
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*

class TradingBotTests : KoinTest {

    companion object {

        val defaultStockAggregationRequest = StockAggregationRequest(
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

        val testModule = module {
            single<AlpacaRepository> { AlpacaRepository() }
            single<TraderService> { TraderService() }
            single { TradingBot() }
        }
    }

    @AfterTest
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


        // Preconditions
        val stockAggregationRequest = defaultStockAggregationRequest.copy()
        val tradingBot by inject<TradingBot>()

        val resultWithLessData = runBlocking {
            tradingBot.backtest(Strategies.MovingAverage, stockAggregationRequest)
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
    fun `Backtesting with default stockAggregation`() {
        // This data set provokes one buy, therefore winRate stays default value
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
                        timestamp = "2024-01-03T00:03:00Z",
                        volume = 650,
                        vwap = 185.26
                    ),
                    StockBar(
                        close = 185.24,
                        high = 185.24,
                        low = 185.24,
                        trades = 40,
                        open = 185.24,
                        timestamp = "2024-01-03T00:04:00Z",
                        volume = 982,
                        vwap = 185.24
                    ),
                    StockBar(
                        close = 185.24,
                        high = 185.24,
                        low = 185.24,
                        trades = 30,
                        open = 185.24,
                        timestamp = "2024-01-03T00:05:00Z",
                        volume = 2718,
                        vwap = 185.24
                    ),
                    StockBar(
                        close = 185.30,
                        high = 185.35,
                        low = 185.25,
                        trades = 50,
                        open = 185.27,
                        timestamp = "2024-01-03T00:06:00Z",
                        volume = 1200,
                        vwap = 185.29
                    ),
                    StockBar(
                        close = 185.32,
                        high = 185.38,
                        low = 185.28,
                        trades = 45,
                        open = 185.30,
                        timestamp = "2024-01-03T00:07:00Z",
                        volume = 1100,
                        vwap = 185.32
                    ),
                    StockBar(
                        close = 185.33,
                        high = 185.40,
                        low = 185.30,
                        trades = 42,
                        open = 185.31,
                        timestamp = "2024-01-03T00:08:00Z",
                        volume = 1300,
                        vwap = 185.34
                    ),
                    StockBar(
                        close = 185.35,
                        high = 185.45,
                        low = 185.33,
                        trades = 48,
                        open = 185.32,
                        timestamp = "2024-01-03T00:09:00Z",
                        volume = 1400,
                        vwap = 185.36
                    ),
                    StockBar(
                        close = 185.34,
                        high = 185.46,
                        low = 185.31,
                        trades = 39,
                        open = 185.33,
                        timestamp = "2024-01-03T00:10:00Z",
                        volume = 1250,
                        vwap = 185.35
                    ),
                    StockBar(
                        close = 185.36,
                        high = 185.48,
                        low = 185.34,
                        trades = 55,
                        open = 185.34,
                        timestamp = "2024-01-03T00:11:00Z",
                        volume = 1600,
                        vwap = 185.38
                    ),
                    StockBar(
                        close = 185.37,
                        high = 185.50,
                        low = 185.36,
                        trades = 60,
                        open = 185.35,
                        timestamp = "2024-01-03T00:12:00Z",
                        volume = 1700,
                        vwap = 185.39
                    ),
                    StockBar(
                        close = 185.39,
                        high = 185.52,
                        low = 185.37,
                        trades = 62,
                        open = 185.37,
                        timestamp = "2024-01-03T00:13:00Z",
                        volume = 1800,
                        vwap = 185.41
                    ),
                    StockBar(
                        close = 185.38,
                        high = 185.51,
                        low = 185.36,
                        trades = 57,
                        open = 185.38,
                        timestamp = "2024-01-03T00:14:00Z",
                        volume = 1500,
                        vwap = 185.40
                    ),
                    StockBar(
                        close = 185.40,
                        high = 185.53,
                        low = 185.39,
                        trades = 58,
                        open = 185.38,
                        timestamp = "2024-01-03T00:15:00Z",
                        volume = 1550,
                        vwap = 185.42
                    ),
                    StockBar(
                        close = 185.41,
                        high = 185.55,
                        low = 185.40,
                        trades = 54,
                        open = 185.40,
                        timestamp = "2024-01-03T00:16:00Z",
                        volume = 1650,
                        vwap = 185.43
                    ),
                    StockBar(
                        close = 185.43,
                        high = 185.57,
                        low = 185.41,
                        trades = 53,
                        open = 185.42,
                        timestamp = "2024-01-03T00:17:00Z",
                        volume = 1750,
                        vwap = 185.45
                    ),
                    StockBar(
                        close = 185.42,
                        high = 185.56,
                        low = 185.40,
                        trades = 59,
                        open = 185.43,
                        timestamp = "2024-01-03T00:18:00Z",
                        volume = 1600,
                        vwap = 185.44
                    ),
                    StockBar(
                        close = 185.44,
                        high = 185.58,
                        low = 185.42,
                        trades = 61,
                        open = 185.44,
                        timestamp = "2024-01-03T00:19:00Z",
                        volume = 1700,
                        vwap = 185.46
                    ),
                    StockBar(
                        close = 185.45,
                        high = 185.60,
                        low = 185.44,
                        trades = 63,
                        open = 185.45,
                        timestamp = "2024-01-03T00:20:00Z",
                        volume = 1800,
                        vwap = 185.47
                    ),
                    StockBar(
                        close = 185.47,
                        high = 185.62,
                        low = 185.45,
                        trades = 65,
                        open = 185.46,
                        timestamp = "2024-01-03T00:21:00Z",
                        volume = 1900,
                        vwap = 185.49
                    ),
                    StockBar(
                        close = 185.46,
                        high = 185.61,
                        low = 185.44,
                        trades = 64,
                        open = 185.47,
                        timestamp = "2024-01-03T00:22:00Z",
                        volume = 1850,
                        vwap = 185.48
                    ),
                    StockBar(
                        close = 185.48,
                        high = 185.63,
                        low = 185.46,
                        trades = 66,
                        open = 185.48,
                        timestamp = "2024-01-03T00:23:00Z",
                        volume = 1950,
                        vwap = 185.50
                    ),
                    StockBar(
                        close = 185.49,
                        high = 185.65,
                        low = 185.47,
                        trades = 68,
                        open = 185.49,
                        timestamp = "2024-01-03T00:24:00Z",
                        volume = 2000,
                        vwap = 185.51
                    ),
                    StockBar(
                        close = 185.50,
                        high = 185.66,
                        low = 185.48,
                        trades = 70,
                        open = 185.50,
                        timestamp = "2024-01-03T00:25:00Z",
                        volume = 2100,
                        vwap = 185.52
                    ),
                    StockBar(
                        close = 185.52,
                        high = 185.67,
                        low = 185.50,
                        trades = 72,
                        open = 185.51,
                        timestamp = "2024-01-03T00:26:00Z",
                        volume = 2200,
                        vwap = 185.54
                    ),
                    StockBar(
                        close = 185.55,
                        high = 185.70,
                        low = 185.53,
                        trades = 75,
                        open = 185.52,
                        timestamp = "2024-01-03T00:27:00Z",
                        volume = 2300,
                        vwap = 185.56
                    ),
                    StockBar(
                        close = 185.57,
                        high = 185.72,
                        low = 185.54,
                        trades = 77,
                        open = 185.55,
                        timestamp = "2024-01-03T00:28:00Z",
                        volume = 2400,
                        vwap = 185.59
                    ),
                    StockBar(
                        close = 185.60,
                        high = 185.75,
                        low = 185.58,
                        trades = 80,
                        open = 185.57,
                        timestamp = "2024-01-03T00:29:00Z",
                        volume = 2500,
                        vwap = 185.61
                    ),
                    StockBar(
                        close = 185.62,
                        high = 185.77,
                        low = 185.60,
                        trades = 83,
                        open = 185.60,
                        timestamp = "2024-01-03T00:30:00Z",
                        volume = 2600,
                        vwap = 185.64
                    ),
                    StockBar(
                        close = 185.65,
                        high = 185.80,
                        low = 185.63,
                        trades = 85,
                        open = 185.62,
                        timestamp = "2024-01-03T00:31:00Z",
                        volume = 2700,
                        vwap = 185.67
                    ),
                    StockBar(
                        close = 185.67,
                        high = 185.82,
                        low = 185.65,
                        trades = 88,
                        open = 185.65,
                        timestamp = "2024-01-03T00:32:00Z",
                        volume = 2800,
                        vwap = 185.69
                    ),
                    StockBar(
                        close = 185.70,
                        high = 185.85,
                        low = 185.68,
                        trades = 90,
                        open = 185.67,
                        timestamp = "2024-01-03T00:33:00Z",
                        volume = 2900,
                        vwap = 185.72
                    ),
                    StockBar(
                        close = 185.72,
                        high = 185.87,
                        low = 185.70,
                        trades = 92,
                        open = 185.70,
                        timestamp = "2024-01-03T00:34:00Z",
                        volume = 3000,
                        vwap = 185.74
                    ),
                    StockBar(
                        close = 185.75,
                        high = 185.90,
                        low = 185.73,
                        trades = 95,
                        open = 185.72,
                        timestamp = "2024-01-03T00:35:00Z",
                        volume = 3100,
                        vwap = 185.77
                    ),
                    StockBar(
                        close = 185.77,
                        high = 185.92,
                        low = 185.75,
                        trades = 97,
                        open = 185.75,
                        timestamp = "2024-01-03T00:36:00Z",
                        volume = 3200,
                        vwap = 185.79
                    ),
                    StockBar(
                        close = 185.80,
                        high = 185.95,
                        low = 185.78,
                        trades = 100,
                        open = 185.77,
                        timestamp = "2024-01-03T00:37:00Z",
                        volume = 3300,
                        vwap = 185.82
                    ),
                    StockBar(
                        close = 185.82,
                        high = 185.97,
                        low = 185.80,
                        trades = 102,
                        open = 185.80,
                        timestamp = "2024-01-03T00:38:00Z",
                        volume = 3400,
                        vwap = 185.84
                    ),
                    StockBar(
                        close = 185.85,
                        high = 186.00,
                        low = 185.83,
                        trades = 105,
                        open = 185.82,
                        timestamp = "2024-01-03T00:39:00Z",
                        volume = 3500,
                        vwap = 185.87
                    ),
                    StockBar(
                        close = 185.87,
                        high = 186.02,
                        low = 185.85,
                        trades = 107,
                        open = 185.85,
                        timestamp = "2024-01-03T00:40:00Z",
                        volume = 3600,
                        vwap = 185.89
                    ),
                    StockBar(
                        close = 185.90,
                        high = 186.05,
                        low = 185.88,
                        trades = 110,
                        open = 185.87,
                        timestamp = "2024-01-03T00:41:00Z",
                        volume = 3700,
                        vwap = 185.92
                    ),
                    StockBar(
                        close = 185.92,
                        high = 186.07,
                        low = 185.90,
                        trades = 112,
                        open = 185.90,
                        timestamp = "2024-01-03T00:42:00Z",
                        volume = 3800,
                        vwap = 185.94
                    ),
                    StockBar(
                        close = 185.95,
                        high = 186.10,
                        low = 185.93,
                        trades = 115,
                        open = 185.92,
                        timestamp = "2024-01-03T00:43:00Z",
                        volume = 3900,
                        vwap = 185.97
                    ),
                    StockBar(
                        close = 185.97,
                        high = 186.12,
                        low = 185.95,
                        trades = 117,
                        open = 185.95,
                        timestamp = "2024-01-03T00:44:00Z",
                        volume = 4000,
                        vwap = 185.99
                    ),
                    StockBar(
                        close = 186.00,
                        high = 186.15,
                        low = 185.98,
                        trades = 120,
                        open = 185.97,
                        timestamp = "2024-01-03T00:45:00Z",
                        volume = 4100,
                        vwap = 186.02
                    ),
                    StockBar(
                        close = 186.02,
                        high = 186.17,
                        low = 186.00,
                        trades = 122,
                        open = 186.00,
                        timestamp = "2024-01-03T00:46:00Z",
                        volume = 4200,
                        vwap = 186.04
                    ),
                    StockBar(
                        close = 186.05,
                        high = 186.20,
                        low = 186.03,
                        trades = 125,
                        open = 186.02,
                        timestamp = "2024-01-03T00:47:00Z",
                        volume = 4300,
                        vwap = 186.07
                    ),
                    StockBar(
                        close = 186.07,
                        high = 186.22,
                        low = 186.05,
                        trades = 127,
                        open = 186.05,
                        timestamp = "2024-01-03T00:48:00Z",
                        volume = 4400,
                        vwap = 186.09
                    ),
                    StockBar(
                        close = 186.10,
                        high = 186.25,
                        low = 186.08,
                        trades = 130,
                        open = 186.07,
                        timestamp = "2024-01-03T00:49:00Z",
                        volume = 4500,
                        vwap = 186.12
                    ),
                    StockBar(
                        close = 186.12,
                        high = 186.27,
                        low = 186.10,
                        trades = 132,
                        open = 186.10,
                        timestamp = "2024-01-03T00:50:00Z",
                        volume = 4600,
                        vwap = 186.14
                    ),
                    StockBar(
                        close = 186.15,
                        high = 186.30,
                        low = 186.13,
                        trades = 135,
                        open = 186.12,
                        timestamp = "2024-01-03T00:51:00Z",
                        volume = 4700,
                        vwap = 186.17
                    ),
                    StockBar(
                        close = 186.17,
                        high = 186.32,
                        low = 186.15,
                        trades = 137,
                        open = 186.15,
                        timestamp = "2024-01-03T00:52:00Z",
                        volume = 4800,
                        vwap = 186.19
                    ),
                    StockBar(
                        close = 186.20,
                        high = 186.35,
                        low = 186.18,
                        trades = 140,
                        open = 186.17,
                        timestamp = "2024-01-03T00:53:00Z",
                        volume = 4900,
                        vwap = 186.22
                    ),
                    StockBar(
                        close = 186.22,
                        high = 186.37,
                        low = 186.20,
                        trades = 142,
                        open = 186.20,
                        timestamp = "2024-01-03T00:54:00Z",
                        volume = 5000,
                        vwap = 186.24
                    ),
                    StockBar(
                        close = 186.25,
                        high = 186.40,
                        low = 186.23,
                        trades = 145,
                        open = 186.22,
                        timestamp = "2024-01-03T00:55:00Z",
                        volume = 5100,
                        vwap = 186.27
                    ),
                    StockBar(
                        close = 186.27,
                        high = 186.42,
                        low = 186.25,
                        trades = 147,
                        open = 186.25,
                        timestamp = "2024-01-03T00:56:00Z",
                        volume = 5200,
                        vwap = 186.29
                    ),
                    StockBar(
                        close = 186.30,
                        high = 186.45,
                        low = 186.28,
                        trades = 150,
                        open = 186.27,
                        timestamp = "2024-01-03T00:57:00Z",
                        volume = 5300,
                        vwap = 186.32
                    ),
                    StockBar(
                        close = 186.32,
                        high = 186.47,
                        low = 186.30,
                        trades = 152,
                        open = 186.30,
                        timestamp = "2024-01-03T00:58:00Z",
                        volume = 5400,
                        vwap = 186.34
                    ),
                    StockBar(
                        close = 186.35,
                        high = 186.50,
                        low = 186.33,
                        trades = 155,
                        open = 186.32,
                        timestamp = "2024-01-03T00:59:00Z",
                        volume = 5500,
                        vwap = 186.37
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
                    assertNotEquals(defaultBackTestResult.roiPercent, resultValue.roiPercent)
                    assertEquals(defaultBackTestResult.winRatePercent, resultValue.winRatePercent)
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

        val request = StockAggregationRequest()
        request.symbols = ""
        val tradingBot by inject<TradingBot>()
        val resultWithEmptySymbol = runBlocking {
            tradingBot.backtest(Strategies.MovingAverage, request)
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