package com.example.tradingLogic

import com.example.services.TraderService
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.Strategies
import com.example.tradingLogic.strategies.StrategyFactory
import com.example.tradingLogic.strategies.TradingSignal
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory


class TradingBot : KoinComponent {

    companion object {
        private val log = LoggerFactory.getLogger(TradingBot::class.java)
    }

    private val mTraderService by inject<TraderService>()
    private var mJob: Job? = null
    private val mBotScope = CoroutineScope(Dispatchers.IO)
    var mIndicators = Indicators()
        private set
    var mBacktestIndicators = Indicators()
        private set
    private var mStrategy = StrategyFactory().createStrategy(Strategies.None)
    private var mOrderRequest = OrderRequest()
    private var mStockAggregationRequest = StockAggregationRequest()
    private var mTimeframe = StockAggregationRequest().timeframe

    suspend fun backtest(
        strategySelector: Strategies,
        stockAggregationRequest: StockAggregationRequest
    ): Result<BacktestResult, TradingLogicError> {
        if (strategySelector == Strategies.None) {
            return Result.Error(TradingLogicError.StrategyError.NO_STRATEGY_SELECTED)
        }
        setStrategy(strategySelector)
        val symbol = stockAggregationRequest.symbols
        if (symbol.isEmpty()) {
            return Result.Error(TradingLogicError.StrategyError.NO_SYMBOLS_PROVIDED)
        }
        mBacktestIndicators.mStock = symbol

        val initialBalance = 10000.0 // Starting capital
        var balance = initialBalance
        val positionSize = 10
        var positions = 0

        var entryPrice: Double? = null
        var closedTrades = 0
        var winningTrades = 0

        val bars = when (val result = getValidatedHistoricalBars(stockAggregationRequest, mBacktestIndicators)) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> {
                result.data.ifEmpty {
                    return Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE)
                }
            }
        }

        when (val result = mBacktestIndicators.updateIndicators(bars)) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> Unit
        }

        mBacktestIndicators.mOriginalPrices.forEachIndexed { index, originalPrice ->
            val tradingSignal = when (val indicatorPointsResult = mBacktestIndicators.getIndicatorPoints(index)) {
                is Result.Error -> return Result.Error(indicatorPointsResult.error)
                is Result.Success -> mStrategy.executeAlgorithm(indicatorPointsResult.data)
            }
            when (tradingSignal) {
                TradingSignal.Buy -> {
                    if (positions == 0) {
                        val cost = positionSize * originalPrice
                        if (balance >= cost) {
                            positions = positionSize
                            entryPrice = originalPrice
                            balance -= cost
                            log.info("Buy qty=$positionSize at price=$originalPrice, cost=$cost")
                        } else {
                            log.info("Buy skipped (insufficient balance). Needed=$cost, balance=$balance")
                        }
                    }
                }

                TradingSignal.Sell -> {
                    if (positions == positionSize && entryPrice != null) {
                        log.info("Closing position at price: $originalPrice, entry price was: $entryPrice")
                        val pnl = (originalPrice - entryPrice!!) * positionSize
                        closedTrades += 1
                        if (pnl > 0.0) {
                            winningTrades += 1
                        }

                        positions = 0
                        entryPrice = null
                        balance += positionSize * originalPrice
                    }
                }

                TradingSignal.Hold -> {
                    // Do nothing
                }
            }
        }
        val finalBalance = balance + positions * mBacktestIndicators.mOriginalPrices.last()
        val roiPercent = (finalBalance - initialBalance) / initialBalance * 100
        val winRatePercent = if (closedTrades == 0) 0.0 else (winningTrades.toDouble() / closedTrades) * 100.0
        log.info("Final position: $positions, ROI%: $roiPercent, Final Balance: $finalBalance, winrate%: $winRatePercent")
        return Result.Success(
            BacktestResult(
                strategyName = strategySelector,
                finalBalance = finalBalance,
                roiPercent = roiPercent,
                winRatePercent = winRatePercent,
                positions = positions
            )
        )
    }

    fun run(): Result<Unit, TradingLogicError> {
        if (mJob != null && mJob!!.isActive) {
            return Result.Error(TradingLogicError.RunError.ALREADY_RUNNING)
        }
        val delayInMs = parseTimeframeToMillis(mTimeframe)
            ?: return Result.Error(TradingLogicError.RunError.TIME_FRAME_COULD_NOT_PARSED)

        mJob = mBotScope.async<Result<Unit, TradingLogicError>> {
            while (isActive) {
                when (val result = getAccountBalance()) {
                    is Result.Error -> return@async Result.Error(result.error)
                    is Result.Success -> mOrderRequest.quantity =
                        result.data.toInt().toString() // TODO: check here the ratio how much it hsould be transferred
                }
                when (val result = getValidatedHistoricalBars(mStockAggregationRequest, mIndicators)) {
                    is Result.Error -> return@async Result.Error(result.error)
                    is Result.Success -> mIndicators.updateIndicators(result.data)
                }

                val tradingSignal = when (val result = mIndicators.getIndicatorPoints(-1)) {
                    is Result.Error -> return@async Result.Error(result.error)
                    is Result.Success -> mStrategy.executeAlgorithm(result.data)
                }
                when (tradingSignal) {
                    TradingSignal.Buy -> {
                        when (val result = createHandledOrder("buy")) {
                            is Result.Success -> { /* Do Nothing */
                            }

                            is Result.Error -> return@async Result.Error(result.error)
                        }
                    }

                    TradingSignal.Sell -> {
                        when (val result = createHandledOrder("sell")) { // TODO: check mOrderRequest
                            is Result.Success -> { /* Do Nothing */
                            }

                            is Result.Error -> return@async Result.Error(result.error)
                        }
                    }

                    TradingSignal.Hold -> { /* Do nothing */
                    }
                }
                delay(delayInMs)
            }
            return@async Result.Success(Unit)
        }
        return Result.Success(Unit)
    }

    fun stop() {
        mJob?.cancel()
    }

    private suspend fun createHandledOrder(side: String): Result<Unit, TradingLogicError> {
        if (side != "sell" && side != "buy") {
            return Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
        }
        mOrderRequest.side = side
        return mTraderService.createOrder(mOrderRequest) // TODO: check mOrderRequest

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

    private suspend fun getValidatedHistoricalBars(
        stockAggregationRequest: StockAggregationRequest,
        indicators: Indicators
    ): Result<List<StockBar>, TradingLogicError> =
        mTraderService.getHistoricalData(stockAggregationRequest, indicators.mStock)

    private suspend fun getAccountBalance(): Result<Double, TradingLogicError> {
        when (val accountDetails = mTraderService.getAccountDetails()) {
            is Result.Error -> return Result.Error(accountDetails.error)
            is Result.Success -> {
                val balanceStr = accountDetails.data.buyingPower
                val balance = balanceStr.toDoubleOrNull()
                    ?: return Result.Error(TradingLogicError.DataError.MISC_ERROR)
                return Result.Success(balance)
            }
        }

    }

    private fun setStrategy(strategySelector: Strategies) {
        mStrategy = StrategyFactory().createStrategy(strategySelector)
    }

}

