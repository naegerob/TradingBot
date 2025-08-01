package com.example.tradingLogic

import com.example.data.TradingRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class TradingBot : KoinComponent {

    private val mRepository by inject<TradingRepository>()
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

    fun setStockAggregationRequest(stockAggregationRequest: StockAggregationRequest) {
        mStockAggregationRequest = stockAggregationRequest
    }

    fun setOrderRequest(orderRequest: OrderRequest) {
        mOrderRequest = orderRequest
    }

    suspend fun backtest(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest) : Result<Any, TradingLogicError> {
        if(strategySelector == Strategies.None) {
            return Result.Error(TradingLogicError.StrategyError.NO_STRATEGY_SELECTED)
        }
        setStrategy(strategySelector)
        mBacktestIndicators.mStock = stockAggregationRequest.symbols
        if(mBacktestIndicators.mStock.isEmpty()) {
            return Result.Error(TradingLogicError.StrategyError.NO_SYMBOLS_PROVIDED)
        }

        val initialBalance = 10000.0 // Starting capital
        var balance = initialBalance
        val positionSize = 10 // Money per trade
        var positions = 0

        val job : Deferred<Result<Any, TradingLogicError>> = CoroutineScope(Dispatchers.IO).async {

            val bars = when (val result = getValidatedHistoricalBars(stockAggregationRequest, mBacktestIndicators)) {
                is Result.Error -> return@async Result.Error(result.error)
                is Result.Success -> {
                    result.data.ifEmpty {
                        return@async Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE)
                    }
                }
            }

            when (val result = mBacktestIndicators.updateIndicators(bars)) {
                is Result.Error         -> return@async Result.Error(result.error)
                is Result.Success       -> { }
            }


            mBacktestIndicators.mLongSMA.forEachIndexed { index, originalPrice ->
                val tradingSignal = when(val result = mBacktestIndicators.getIndicatorPoints(index)) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> mStrategy.executeAlgorithm(result.data)
                }
                when (tradingSignal) {
                    TradingSignal.Buy -> {
                        if (positions == 0) {
                            println("BUY at $originalPrice")
                            positions = positionSize
                            balance -= positionSize * originalPrice
                        }
                    }
                    TradingSignal.Sell -> {
                        if (positions == positionSize) {
                            println("SELL at $originalPrice")
                            positions = 0
                            balance += positionSize * originalPrice
                        }
                    }
                    TradingSignal.Hold -> {
                        println("Hold Position at $originalPrice")
                    }
                }
            }
            println("Final position: $positions")
            val finalBalance = balance + positions * mBacktestIndicators.mOriginalPrices.last()
            val winRateInPercent = (finalBalance - initialBalance) / finalBalance * 100
            Result.Success(BacktestResult(strategySelector, finalBalance, winRateInPercent, positions))
        }
        return when (val result = job.await()) {
            is Result.Error<*, *> -> Result.Error(result.error as TradingLogicError)
            is Result.Success<*, *> -> Result.Success(result.data!!)
        }
    }

    fun run() : Result<Unit, TradingLogicError> {
        if (mIsRunning) {
            return Result.Error(TradingLogicError.RunError.ALREADY_RUNNING)
        }
        val delayInMs = parseTimeframeToMillis(mTimeframe)
            ?: return Result.Error(TradingLogicError.RunError.TIME_FRAME_COULD_NOT_PARSED)
        mIsRunning = true
        mJob = CoroutineScope(Dispatchers.IO).async<Result<Unit, TradingLogicError>> {
            while(mIsRunning) {
                when (val result = getAccountBalance()) {
                    is Result.Error ->  return@async Result.Error(result.error)
                    is Result.Success -> mOrderRequest.quantity = result.data.toInt().toString() // TODO: check here the ratio how much it hsould be transferred
                }
                when(val result = getValidatedHistoricalBars(mStockAggregationRequest, mIndicators)) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> mIndicators.updateIndicators(result.data)
                }

                val tradingSignal = when(val result = mIndicators.getIndicatorPoints(-1)) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> mStrategy.executeAlgorithm(result.data)
                }
                when(tradingSignal) {
                    TradingSignal.Buy -> {
                        when (val result = createHandledOrder("buy")) {
                            is Result.Success   -> { /* Do Nothing */ }
                            is Result.Error     -> return@async Result.Error(result.error)
                        }
                    }
                    TradingSignal.Sell -> {
                        when (val result = createHandledOrder("sell")) { // TODO: check mOrderRequest
                            is Result.Success   -> { /* Do Nothing */ }
                            is Result.Error     -> return@async Result.Error(result.error)
                        }
                    }
                    TradingSignal.Hold -> { /* Do nothing */ }
                }
                delay(delayInMs)
            }
            return@async Result.Success(Unit)
        }
        return Result.Success(Unit)
    }

    fun stop() {
        mJob?.cancel()
        mIsRunning = false
    }

    private suspend fun createHandledOrder(side: String) : Result<Unit, TradingLogicError> {
        if (side != "sell" && side != "buy") {
            return Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
        }
        mOrderRequest.side = side
        val httpResponse = mRepository.createOrder(mOrderRequest) // TODO: check mOrderRequest
        return when (httpResponse.status) {
            HttpStatusCode.OK                   -> Result.Success(Unit)
            HttpStatusCode.Forbidden            -> Result.Error(TradingLogicError.DataError.NO_SUFFICIENT_ACCOUNT_BALANCE)
            HttpStatusCode.UnprocessableEntity  -> Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
            else                                -> Result.Error(TradingLogicError.DataError.MISC_ERROR)
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

    private suspend fun getValidatedHistoricalBars(stockAggregationRequest: StockAggregationRequest, indicators: Indicators) : Result<List<StockBar>, TradingLogicError> {
        val httpResponse = mRepository.getHistoricalData(stockAggregationRequest)
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val stockResponse = httpResponse.body<StockAggregationResponse>()
                if (stockResponse.bars[indicators.mStock] == null) {
                    return Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE)
                }
                return Result.Success(stockResponse.bars[indicators.mStock]!!)
            }
            HttpStatusCode.TooManyRequests  -> return Result.Error(TradingLogicError.DataError.HISTORICAL_DATA_TOO_MANY_REQUESTS)
            HttpStatusCode.BadRequest       -> return Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
            HttpStatusCode.Unauthorized     -> return Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
            else                            -> return Result.Error(TradingLogicError.DataError.MISC_ERROR)
        }
    }

    private suspend fun getAccountBalance() : Result<Double, TradingLogicError> {
        val httpResponse = mRepository.getAccountDetails()
        return when (httpResponse.status) {
            HttpStatusCode.OK   -> Result.Success(httpResponse.body<Account>().buyingPower.toDouble())
            else                -> Result.Error(TradingLogicError.DataError.NO_SUFFICIENT_ACCOUNT_BALANCE)
        }
    }

    private fun setStrategy(strategySelector: Strategies) {
        mStrategy = StrategyFactory().createStrategy(strategySelector)
    }

}

