package com.example.tradingLogic

import com.example.data.AlpacaRepository
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.singleModels.StockAggregationResponse
import com.example.data.singleModels.StockBar
import com.example.tradingLogic.strategies.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*


class TradingBot(
    private val mAlpacaRepository: AlpacaRepository
) {

    @Volatile
    private var mIsRunning = false
    private var mJob: Job? = null
    private var mIndicators = Indicators()
    private var mStrategy = StrategyFactory().createStrategy(Strategies.none)
    private var mOrderRequest = OrderRequest()
    private var mStockAggregationRequest = StockAggregationRequest()
    private var mTimeframe = StockAggregationRequest().timeframe

    private suspend fun getValidatedHistoricalBars(stockAggregationRequest: StockAggregationRequest, indicators: Indicators): List<StockBar> {
        val httpResponse = mAlpacaRepository.getHistoricalData(stockAggregationRequest)

        try {
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val stockResponse = httpResponse.body<StockAggregationResponse>()
                    return stockResponse.bars[indicators.mStock]!!
                }
                else -> return emptyList() // TODO: is just bad
            }
        } catch (e: Exception) {
            println(e)
            return emptyList()
        }
    }

    fun setStrategy(strategySelector: Strategies) {
        mStrategy = StrategyFactory().createStrategy(strategySelector)
    }

    fun setStockAggregationRequest(stockAggregationRequest: StockAggregationRequest) {
        mStockAggregationRequest = stockAggregationRequest
    }

    fun setOrderRequest(orderRequest: OrderRequest) {
        mOrderRequest = orderRequest
    }

    fun backtest(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest) {
        val strategy = StrategyFactory().createStrategy(strategySelector)
        val backTestIndicators = Indicators()
        backTestIndicators.mStock = stockAggregationRequest.symbols

        runBlocking {
            val historicalBars = getValidatedHistoricalBars(stockAggregationRequest, backTestIndicators)
            backTestIndicators.updateIndicators(historicalBars)
            for (originalPrice in backTestIndicators.mOriginalPrices) {
                val signal = strategy.executeAlgorithm(backTestIndicators)
                if (signal == TradingSignal.BUY) {
                    println("BUY at $originalPrice")
                } else if (signal == TradingSignal.SELL) {
                    println("SELL at $originalPrice")
                }
            }
        }
    }

    fun run() {
        if (mIsRunning) return
        val delayInMs = parseTimeframeToMillis(mTimeframe) ?: return

        mIsRunning = true

        mJob = CoroutineScope(Dispatchers.IO).launch {
            while(mIsRunning) {
                val historicalBars = getValidatedHistoricalBars(mStockAggregationRequest, mIndicators)
                mIndicators.updateIndicators(historicalBars)
                val signal = mStrategy.executeAlgorithm(mIndicators)
                when(signal) {
                    TradingSignal.BUY -> {
                        mOrderRequest.side = "buy"
                        mAlpacaRepository.createOrder(mOrderRequest)
                    }
                    TradingSignal.SELL -> {
                        mOrderRequest.side = "sell"
                        mAlpacaRepository.createOrder(mOrderRequest)
                    }
                    TradingSignal.HOLD -> { /* Do nothing */ }
                }
                delay(delayInMs)

            }
        }
    }

    private fun parseTimeframeToMillis(timeframe: String): Long? {
        val regex = """(\d+)(Min|T|Hour|H|Day|D|Week|W|Month|M)""".toRegex()
        val match = regex.matchEntire(timeframe) ?: return null

        val (valueStr, unit) = match.destructured
        val value = valueStr.toIntOrNull() ?: return null

        return when (unit) {
            "Min", "T" -> value * 60_000L // Minutes
            "Hour", "H" -> value * 60 * 60_000L // Hours
            "Day", "D" -> value * 24 * 60 * 60_000L // Days
            "Week", "W" -> value * 7 * 24 * 60 * 60_000L // Weeks
            "Month", "M" -> value * 30 * 24 * 60 * 60_000L // Approximate months
            else -> null
        }
    }

    fun stop() {
        mJob?.cancel()
        mIsRunning = false
    }
}

