package com.example.tradingLogic

import com.example.data.AlpacaRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException


class TradingBot(
    private val mAlpacaRepository: AlpacaRepository
) {

    @Volatile
    private var mIsRunning = false
    private var mJob: Job? = null
    private var mIndicators = Indicators()
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

    fun backtest(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest): BacktestResult {
        val strategy = StrategyFactory().createStrategy(strategySelector)
        val backTestIndicators = Indicators()
        backTestIndicators.mStock = stockAggregationRequest.symbols

        var balance = 10000.0 // Starting capital
        val positionSize = 100 // Money per trade
        var wins = 0
        var losses = 0
        var maxDrawdown = 0.0
        var peakBalance = balance
        val returns = mutableListOf<Double>()

        runBlocking {
            val historicalBars = getValidatedHistoricalBars(stockAggregationRequest, backTestIndicators)
            backTestIndicators.updateIndicators(historicalBars)
            for (originalPrice in backTestIndicators.mOriginalPrices) {
                val signal = strategy.executeAlgorithm(backTestIndicators)
                if (signal == TradingSignal.Buy) {
                    println("BUY at $originalPrice")

                    balance -= positionSize * originalPrice

                } else if (signal == TradingSignal.Sell) {
                    println("SELL at $originalPrice")
                    val profit = positionSize * originalPrice
                    balance += profit
                    val tradeReturn = (profit - (positionSize * originalPrice)) / (positionSize * originalPrice)
                    returns.add(tradeReturn)

                    if (tradeReturn > 0) wins++ else losses++
                }
            }

            // Compute performance metrics
            val totalReturn = (balance - 10000) / 10000 * 100
            val winRate = if (wins + losses > 0) wins.toDouble() / (wins + losses) * 100 else 0.0
            val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0

            return@runBlocking BacktestResult(strategySelector, totalReturn, avgReturn, winRate)
        }
        return BacktestResult()
    }

    fun run() {
        if (mIsRunning) return
        val delayInMs = parseTimeframeToMillis(mTimeframe) ?: return

        mIsRunning = true

        mJob = CoroutineScope(Dispatchers.IO).launch {
            while(mIsRunning) {
                val accountBalance = getAccountBalance().getOrNull() // TODO: implement
                val historicalBars = getValidatedHistoricalBars(mStockAggregationRequest, mIndicators)
                mIndicators.updateIndicators(historicalBars)
                val signal = mStrategy.executeAlgorithm(mIndicators)
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

