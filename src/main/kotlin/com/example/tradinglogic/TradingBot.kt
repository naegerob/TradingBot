package com.example.tradinglogic

import com.example.services.TraderService
import com.example.data.singleModels.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class TradingBot : KoinComponent {

    companion object {
        private val log = LoggerFactory.getLogger(TradingBot::class.java)
    }

    private val mTraderService by inject<TraderService>()
    private var mJob: Deferred<Result<Unit, TradingLogicError>>? = null
    var mIndicators = Indicators()
        private set
    var mBacktestIndicators = Indicators()
        private set
    private var mStrategy = StrategyFactory().createStrategy(Strategies.None)

    suspend fun backtest(
        backtestConfig: BacktestConfig
    ): Result<BacktestResult, TradingLogicError> {
        val strategySelector = backtestConfig.strategySelector
        val stockAggregationRequest = backtestConfig.stockAggregationRequest
        if (strategySelector == Strategies.None) {
            return Result.Error(TradingLogicError.StrategyError.NO_STRATEGY_SELECTED)
        }
        setStrategy(strategySelector)
        val symbol = stockAggregationRequest.symbols
        if (symbol.isEmpty()) {
            return Result.Error(TradingLogicError.StrategyError.NO_SYMBOLS_PROVIDED)
        }
        mBacktestIndicators.mStock = symbol

        val initialBalance = 10000.0
        val positionSizePerOrder = 10
        var balance = initialBalance
        var positions = 0
        var entryPrice: Double? = null
        var closedTrades = 0
        var winningTrades = 0
        var grossProfit = 0.0
        var grossLoss = 0.0
        var positionState = TradingPosition.Flat // We have no positions yet

        val bars = when (val result = getValidatedHistoricalBars(stockAggregationRequest)) {
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
                is Result.Success -> mStrategy(indicatorPointsResult.data)
            }
            val tradingAction = handleSignal(positionState, tradingSignal)

            when(tradingAction) {
                TradingAction.OpenLong -> {
                    // Buy
                    if (positionState == TradingPosition.Flat) {
                        val costPerTrade = positionSizePerOrder * originalPrice
                        if (balance >= costPerTrade) {
                            positions = positionSizePerOrder
                            entryPrice = originalPrice
                            balance -= costPerTrade
                            positionState = TradingPosition.Long
                            log.info("Open Long. Buy qty=$positionSizePerOrder at price=$originalPrice equals to cost=$costPerTrade")
                        } else {
                            log.info("Open Long. Buy skipped (insufficient balance). Needed=$costPerTrade, balance=$balance")
                        }
                    }
                }
                TradingAction.OpenShort -> {
                    // Sell to open short
                    if (positionState == TradingPosition.Flat) {
                        val costPerTrade = positionSizePerOrder * originalPrice
                        positions = -positionSizePerOrder
                        entryPrice = originalPrice
                        balance += costPerTrade
                        positionState = TradingPosition.Short
                        log.info("Open Short. Sell qty=$positionSizePerOrder at price=$originalPrice equals to costPerTrade=$costPerTrade")
                    }
                }
                TradingAction.CloseLong -> {
                    // Sell
                    if (positionState == TradingPosition.Long && entryPrice != null) {
                        val tradeProfitOrLoss = (originalPrice - entryPrice) * positionSizePerOrder
                        if (tradeProfitOrLoss > 0) {
                            grossProfit += tradeProfitOrLoss
                        } else {
                            grossLoss += abs(tradeProfitOrLoss)
                        }
                        closedTrades += 1
                        if (tradeProfitOrLoss > 0.0) {
                            winningTrades += 1
                        }
                        positions = 0
                        entryPrice = null
                        balance += positionSizePerOrder * originalPrice
                        positionState = TradingPosition.Flat
                        log.info("Close Long. Sell qty=$positionSizePerOrder at price=$originalPrice, tradeProfitOrLoss=$tradeProfitOrLoss")
                    }
                }
                TradingAction.CloseShort -> {
                    // Buy
                    if (positionState == TradingPosition.Short && entryPrice != null) {
                        val costPerTrade = positionSizePerOrder * originalPrice
                        val tradeProfitOrLoss = (entryPrice - originalPrice) * positionSizePerOrder
                        if (tradeProfitOrLoss > 0) {
                            grossProfit += tradeProfitOrLoss
                        } else {
                            grossLoss += abs(tradeProfitOrLoss)
                        }
                        closedTrades += 1
                        if (tradeProfitOrLoss > 0.0) {
                            winningTrades += 1
                        }
                        positions = 0
                        entryPrice = null
                        balance -= costPerTrade
                        positionState = TradingPosition.Flat
                        log.info("Close Short. Buy qty=$positionSizePerOrder at price=$originalPrice, cost=$costPerTrade, tradeProfitOrLoss=$tradeProfitOrLoss")

                    }
                }
                TradingAction.DoNothing -> {}
            }
        }
        val finalEquity = balance + positions * mBacktestIndicators.mOriginalPrices.last()
        val profitFactor = if (grossLoss == 0.0) grossProfit else (grossProfit / grossLoss)
        val profit = finalEquity - initialBalance
        val roiPercent = profit / initialBalance * 100
        val winRatePercent = if (closedTrades == 0) 0.0 else (winningTrades.toDouble() / closedTrades) * 100.0
        log.info("Final position: $positions, positionState: $positionState, ROI%: $roiPercent, Final Balance (Cash): $balance, Final equity (Cash+Positions) $finalEquity, winrate%: $winRatePercent, profit = $profit, profitfactor = $profitFactor")
        return Result.Success(
            BacktestResult(
                strategyName = strategySelector,
                finalEquity = finalEquity,
                profit = profit,
                roiPercent = roiPercent,
                winRatePercent = winRatePercent,
                positions = positions
            )
        )
    }

    suspend fun run(): Result<Unit, TradingLogicError> {
        log.info("Bot started with strategy")
        if (mJob?.isActive == true) {
            return Result.Error(TradingLogicError.RunError.ALREADY_RUNNING)
        }
        val stockAggregationRequest = StockAggregationRequest(
            endDateTime = null,
            startDateTime = null,
            limit = requiredBars(),
            sort = "asc"
        )
        val delayInMs = parseTimeframeToMillis(stockAggregationRequest.timeframe)?.div(2)
            ?: return Result.Error(TradingLogicError.RunError.TIME_FRAME_COULD_NOT_PARSED)


        val orderRequest = OrderRequest(
            symbol = stockAggregationRequest.symbols,
            side = "buy",
            type = "market",
            timeInForce = "gtc",
            notional = null,
            quantity = null
        )

        mJob = CoroutineScope(Dispatchers.IO).async {

            // Set notional to 0.1% of account balance
            when (val result = getAccountBalance()) {
                is Result.Error -> return@async Result.Error(result.error)
                is Result.Success -> {
                    orderRequest.notional = 1000.toString()
                }
            }
            var positionState = TradingPosition.Flat
            var stocks = when (val result = getValidatedHistoricalBars(stockAggregationRequest)) {
                is Result.Error     -> return@async Result.Error(result.error)
                is Result.Success   -> result.data.sortedBy { it.timestamp }
            }
            stockAggregationRequest.limit = 5
            stockAggregationRequest.sort = "desc"
            log.info("mStockAggregation: $stockAggregationRequest")
            while (isActive) {
                when (val result = mTraderService.getMarketOpeningHours()) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> {
                        if (!result.data) {
                            log.info("Market is closed. Waiting...")
                            delay(delayInMs)
                            continue
                        }
                    }
                }
                when (val result = configurePollingRequest(stockAggregationRequest)) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> Unit
                }
                when (val result = getValidatedHistoricalBars(stockAggregationRequest)) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> {
                        log.info("result: ${result.data}")
                        stocks = upsertRollingWindow(
                            current = stocks,
                            incoming = result.data,
                            windowSize = requiredBars()
                        )
                        log.info("mStock: $stocks")
                        when (val update = mIndicators.updateIndicators(stocks)) {
                            is Result.Error     -> return@async Result.Error(update.error)
                            is Result.Success   -> Unit
                        }
                    }
                }

                log.info("getValidatedHistoricalBars called")
                // TODO: Trading signal is evaluated only due to last bar
                val tradingSignal = when (val result = mIndicators.getIndicatorPoints(-1)) {
                    is Result.Error     -> return@async Result.Error(result.error)
                    is Result.Success   -> mStrategy(result.data)
                }
                val tradingAction = handleSignal(positionState, tradingSignal)

                log.info("trading action: $tradingAction, positionState: $positionState, signal: $tradingSignal")
                when (tradingAction) {
                    TradingAction.OpenLong -> {
                        if (positionState == TradingPosition.Flat) {
                            orderRequest.side = "buy"
                            when (val order = createHandledOrder(orderRequest)) {
                                is Result.Error     -> return@async Result.Error(order.error)
                                is Result.Success   -> {
                                    positionState = TradingPosition.Long
                                    log.info("Open Long: order sent")
                                }
                            }
                        }
                    }
                    TradingAction.OpenShort -> {
                        if (positionState == TradingPosition.Flat) {
                            orderRequest.side = "sell"
                            when (val order = createHandledOrder(orderRequest)) {
                                is Result.Error     -> return@async Result.Error(order.error)
                                is Result.Success   -> {
                                    positionState = TradingPosition.Short
                                    log.info("Open Short: order sent")
                                }
                            }
                        }
                    }
                    TradingAction.CloseLong -> {
                        if (positionState == TradingPosition.Long) {
                            orderRequest.side = "sell"
                            when (val order = createHandledOrder(orderRequest)) {
                                is Result.Error     -> return@async Result.Error(order.error)
                                is Result.Success   -> {
                                    positionState = TradingPosition.Flat
                                    log.info("Close Long: order sent")
                                }
                            }
                        }
                    }
                    TradingAction.CloseShort -> {
                        if (positionState == TradingPosition.Short) {
                            orderRequest.side = "buy"
                            when (val order = createHandledOrder(orderRequest)) {
                                is Result.Error     -> return@async Result.Error(order.error)
                                is Result.Success   -> {
                                    positionState = TradingPosition.Flat
                                    log.info("Close Short: order sent")
                                }
                            }
                        }
                    }
                    TradingAction.DoNothing -> {
                        log.info("No trading action taken.")
                    }
                }
                delay(delayInMs)
            }
            return@async Result.Success(Unit)
        }
        return mJob!!.await()
    }

    fun stop() {
        mJob?.cancel()
        mJob = null
    }

    private fun handleSignal(tradingPosition: TradingPosition, tradingSignal: TradingSignal) : TradingAction {
        when (tradingPosition to tradingSignal) {
            (TradingPosition.Flat to TradingSignal.Buy) -> {
                return TradingAction.OpenLong
            }
            (TradingPosition.Flat to TradingSignal.Sell) -> {
                return TradingAction.OpenShort
            }
            (TradingPosition.Long to TradingSignal.Sell) -> {
                return TradingAction.CloseLong
            }
            (TradingPosition.Short to TradingSignal.Buy) -> {
                return TradingAction.CloseShort
            }
            else -> {
                return TradingAction.DoNothing
            }
        }
    }

    private fun configurePollingRequest(stockAggregationRequest: StockAggregationRequest): Result<Unit, TradingLogicError> {
        stockAggregationRequest.let {
            val endTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)

            val deltaTime = parseTimeframeToMillis(it.timeframe)?.times(it.limit)
                ?: return Result.Error(TradingLogicError.RunError.TIME_FRAME_COULD_NOT_PARSED)

            val startTime = endTime.minus(Duration.ofMillis(deltaTime))

            it.startDateTime = startTime.toString()
            it.endDateTime = endTime.toString()
        }
        return Result.Success(Unit)
    }

    private fun upsertRollingWindow(
        current: List<StockBar>,
        incoming: List<StockBar>,
        windowSize: Int
    ): List<StockBar> {
        if (incoming.isEmpty())
            return current

        val sortedIncoming = incoming.sortedBy { it.timestamp }
        log.info("sortedIncoming: $sortedIncoming")
        var updated = current.sortedBy { it.timestamp }
        log.info("updated: $updated")
        for (bar in sortedIncoming) {
            updated = when {
                updated.isEmpty() -> listOf(bar)
                bar.timestamp == updated.last().timestamp -> updated.dropLast(1) + bar   // replace
                bar.timestamp > updated.last().timestamp -> updated + bar               // append
                else -> updated                                                         // out-of-order ignorieren
            }
        }
        return if (updated.size > windowSize) updated.takeLast(windowSize) else updated
    }


    private fun requiredBars(): Int {
        val maxLookback = 200
        val warmup = 50
        return maxLookback + warmup
    }

    private suspend fun createHandledOrder(orderRequest: OrderRequest): Result<Unit, TradingLogicError> {
        return mTraderService.createOrder(orderRequest)
    }

    private fun parseTimeframeToMillis(timeframe: String): Long? {
        val regex = """(\d+)(Min|T|Hour|H|Day|D|Week|W|Month|M)""".toRegex()
        val match = regex.matchEntire(timeframe) ?: return null

        val (valueStr, unit) = match.destructured
        val value = valueStr.toIntOrNull() ?: return null

        return when (unit) {
            "Min", "T" -> value * 60_000L
            "Hour", "H" -> value * 60 * 60_000L
            "Day", "D" -> value * 24 * 60 * 60_000L
            "Week", "W" -> value * 7 * 24 * 60 * 60_000L
            "Month", "M" -> value * 30 * 24 * 60 * 60_000L
            else -> null
        }
    }

    private suspend fun getValidatedHistoricalBars(
        stockAggregationRequest: StockAggregationRequest
    ): Result<List<StockBar>, TradingLogicError> {
        parseTimeframeToMillis(stockAggregationRequest.timeframe)
            ?: return Result.Error(TradingLogicError.RunError.TIME_FRAME_COULD_NOT_PARSED)
        if (stockAggregationRequest.symbols.isEmpty())
            return Result.Error(TradingLogicError.RunError.SYMBOLS_COULD_NOT_PARSED)
        if (stockAggregationRequest.limit <= 0)
            return Result.Error(TradingLogicError.RunError.LIMIT_COULD_NOT_PARSED)
        return mTraderService.getHistoricalData(stockAggregationRequest)
    }

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
