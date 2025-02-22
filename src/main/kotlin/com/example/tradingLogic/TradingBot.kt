package com.example.tradingLogic

import com.example.data.AlpacaRepository
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.singleModels.StockAggregationResponse
import com.example.data.singleModels.StockBar
import com.example.tradingLogic.strategies.MovingAverageStrategy
import com.example.tradingLogic.strategies.TradingSignal
import com.example.tradingLogic.strategies.TradingStrategy
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*


class TradingBot(
    private val mAlpacaRepository: AlpacaRepository
) {

    @Volatile
    private var mIsRunning = false
    private var mJob: Job? = null
    private var mIndicators: Indicators = Indicators()
    private var mStrategy: TradingStrategy = MovingAverageStrategy()
    private val mOrderRequest: OrderRequest = OrderRequest()
    private val mStockAggregationRequest: StockAggregationRequest = StockAggregationRequest()
    private var mTimeframe: String = StockAggregationRequest().timeframe

    private suspend fun getValidatedHistoricalBars(): List<StockBar> {
        val httpResponse = mAlpacaRepository.getHistoricalData(mStockAggregationRequest)

        try {
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val stockResponse = httpResponse.body<StockAggregationResponse>()
                    return stockResponse.bars[mIndicators.mStock]!!
                }
                else -> return emptyList() // TODO: is just bad
            }
        } catch (e: Exception) {
            println(e)
            return emptyList()
        }
    }

    fun setStrategy(strategy: TradingStrategy) {
        mStrategy = strategy
    }

    fun run() {
        if (mIsRunning) return
        val delayInMs = parseTimeframeToMillis(mTimeframe) ?: return

        mIsRunning = true

        mJob = CoroutineScope(Dispatchers.IO).launch {
            while(mIsRunning) {
                val historicalBars = getValidatedHistoricalBars()
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

