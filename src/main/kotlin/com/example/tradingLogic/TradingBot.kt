package com.example.tradingLogic

import com.example.data.TradingRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*


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
    private suspend fun getValidatedHistoricalBars(stockAggregationRequest: StockAggregationRequest, indicators: Indicators): Result<List<StockBar>, TradingLogicError> {
        try {
            val httpResponse = mAlpacaRepository.getHistoricalData(stockAggregationRequest)
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val stockResponse = httpResponse.body<StockAggregationResponse>()
                    if (stockResponse.bars[indicators.mStock] == null) {
                        return Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE)
                    }
                    return Result.Success(stockResponse.bars[indicators.mStock]!!)
                }
                else -> {
                    return Result.Error(TradingLogicError.DataError.MISC_ERROR)
                }
            }
        } catch (e: Exception) {
            println(e)
            return Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_EXCEPTION)
        }
    }

    private suspend fun getAccountBalance(): Result<Double, TradingLogicError> {

        val httpResponse = mAlpacaRepository.getAccountDetails()
        return when (httpResponse.status) {
            HttpStatusCode.OK -> {
                Result.Success(httpResponse.body<Account>().buyingPower.toDouble())
            }
            else -> { // should never be called
                Result.Error(TradingLogicError.DataError.NO_SUFFICIENT_ACCOUNT_BALANCE)
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

    suspend fun backtest(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest): Result<Any, TradingLogicError> {
        val strategy = StrategyFactory().createStrategy(strategySelector)
        mBacktestIndicators.mStock = stockAggregationRequest.symbols

        val initialBalance = 10000.0 // Starting capital
        var balance = initialBalance
        val positionSize = 10 // Money per trade
        var positions = 0

        val job : Deferred<Result<Any, TradingLogicError>> = CoroutineScope(Dispatchers.IO).async {
            when (val result = getValidatedHistoricalBars(stockAggregationRequest, mBacktestIndicators)) {
                is Result.Error -> {
                    println(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE.toString())
                    return@async Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE)
                }
                is Result.Success -> mBacktestIndicators.updateIndicators(result.data)
            }


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
            Result.Success(BacktestResult(strategySelector, finalBalance, winRateInPercent, positions))
        }
        return Result.Success(job.await())
    }

    fun run() {
        if (mIsRunning) return
        val delayInMs = parseTimeframeToMillis(mTimeframe) ?: return

        mIsRunning = true

        mJob = CoroutineScope(Dispatchers.IO).launch {
            while(mIsRunning) {
                when (val result = getAccountBalance()) {
                    is Result.Error -> TODO()
                    is Result.Success -> TODO()
                }
                when(val result = getValidatedHistoricalBars(mStockAggregationRequest, mIndicators)) {
                    is Result.Error -> TODO()
                    is Result.Success -> mIndicators.updateIndicators(result.data)
                }

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

    fun stop() {
        mJob?.cancel()
        mIsRunning = false
    }
}

