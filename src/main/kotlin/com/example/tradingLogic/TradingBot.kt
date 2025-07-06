package com.example.tradingLogic

import com.example.Error
import com.example.data.TradingRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException


class TradingBot(
    private val mAlpacaRepository: TradingRepository
) {

    @Volatile
    private var mIsRunning = false
    private var mJob: Job? = null
    var mIndicators = Indicators()
        private set
    var mBacktestIndicators = Indicators()
        private set
    private var mStrategy = StrategyFactory().createStrategy(Strategies.None)
    private var mOrderRequest = OrderRequest()
    private var mStockAggregationRequest = StockAggregationRequest()
    private var mTimeframe = StockAggregationRequest().timeframe

    // TODO: Consider changing to handling errors similar to getAccountBalance
    private suspend fun getValidatedHistoricalBars(stockAggregationRequest: StockAggregationRequest, indicators: Indicators): List<StockBar> {
        try {
            val httpResponse = mAlpacaRepository.getHistoricalData(stockAggregationRequest)
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

    private suspend fun getAccountBalance(): Result<Double> {
        return runCatching {
            val httpResponse = mAlpacaRepository.getAccountDetails()
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    httpResponse.body<Account>().buyingPower.toDouble()
                }
                else -> {
                    throw SerializationException("Serialization or API request did not work")
                }
            }
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

    suspend fun backtest(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest): com.example.Result<BacktestResult, TradingBotError> {
        val strategy = StrategyFactory().createStrategy(strategySelector)
        mBacktestIndicators.mStock = stockAggregationRequest.symbols

        val initialBalance = 10000.0 // Starting capital
        var balance = initialBalance
        val positionSize = 10 // Money per trade
        var positions = 0

        val job = CoroutineScope(Dispatchers.IO).async {
            val historicalBars = getValidatedHistoricalBars(stockAggregationRequest, mBacktestIndicators)
            if (historicalBars.isEmpty()) {
                return@async BacktestResult(Strategies.None, 0.0, 0.0, 0)
            }
            mBacktestIndicators.updateIndicators(historicalBars)

            mBacktestIndicators.mLongSMA.forEachIndexed { index, originalPrice ->
                val indicatorSnapshot = mBacktestIndicators.getIndicatorPoints(index)
                val tradingSignal = strategy.executeAlgorithm(indicatorSnapshot)
                if (tradingSignal == TradingSignal.Buy && positions == 0) {
                    println("BUY at $originalPrice")
                    positions = positionSize
                    balance -= positionSize * originalPrice

                } else if (tradingSignal == TradingSignal.Sell && positions == positionSize) {
                    println("SELL at $originalPrice")
                    positions = 0
                    balance += positionSize * originalPrice

                } else {
                    println("Hold Position at $originalPrice")
                }
            }
            println("Final position: $positions")
            val finalBalance = balance + positions * mBacktestIndicators.mOriginalPrices.last()
            val winRateInPercent = (finalBalance - initialBalance) / finalBalance * 100
            BacktestResult(strategySelector, finalBalance, winRateInPercent, positions)
        }
        return com.example.Result.Success(job.await())
    }

    fun run() {
        if (mIsRunning) return
        val delayInMs = parseTimeframeToMillis(mTimeframe) ?: return

        mIsRunning = true

        mJob = CoroutineScope(Dispatchers.IO).launch {
            while(mIsRunning) {
                val accountBalance = getAccountBalance().getOrNull()
                val historicalBars = getValidatedHistoricalBars(mStockAggregationRequest, mIndicators)
                if (historicalBars.isEmpty()) {
                    return@launch
                }
                mIndicators.updateIndicators(historicalBars)
                val latestIndicators = mIndicators.getIndicatorPoints()
                val signal = mStrategy.executeAlgorithm(latestIndicators)
                when(signal) {
                    TradingSignal.Buy -> {
                        mOrderRequest.side = "buy"
                        mAlpacaRepository.createOrder(mOrderRequest)
                    }
                    TradingSignal.Sell -> {
                        mOrderRequest.side = "sell"
                        mAlpacaRepository.createOrder(mOrderRequest)
                    }
                    TradingSignal.Hold -> { /* Do nothing */ }
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

    enum class TradingBotError : Error {
        // TODO: check error messages from alpaca / or codes
        NO_DATA_FROMALPACA
    }
    fun stop() {
        mJob?.cancel()
        mIsRunning = false
    }
}

