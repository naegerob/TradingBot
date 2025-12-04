package com.example.tradingLogic


import com.example.data.TradingRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.Strategies
import io.ktor.client.statement.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TradingController : KoinComponent {

    private val mAlpacaRepo by inject<TradingRepository>()

    var mIndicators = Indicators()
        private set

    // TODO: Consider using builder pattern
    val mTradingBot by inject<TradingBot>()

    /************************************************************
    Methods
     ************************************************************/
    fun areValidStockRequestParameter(stockAggregationRequest: StockAggregationRequest): Boolean {
        val isSymbolValid = stockAggregationRequest.symbols.isNotEmpty() && !stockAggregationRequest.symbols.contains(",")
        val isTimeframeValid = timeframes.any { stockAggregationRequest.timeframe.contains(it)}
        val isFeedValid = feeds.any { stockAggregationRequest.feed == it }
        val isSortValid = sorts.any { stockAggregationRequest.sort == it }
        val isCorrectTimeframe = stockAggregationRequest.timeframe.contains(Regex("\\d+(Min|T|Hour|H|Day|D|Week|W|Month|M)"))
        return isSymbolValid && isTimeframeValid && isFeedValid && isSortValid && isCorrectTimeframe
    }

    fun areValidOrderParameter(orderRequest: OrderRequest): Boolean {
        val isSymbolValid = orderRequest.symbol.isNotEmpty() &&
            !orderRequest.symbol.contains(",") &&
            orderRequest.symbol.matches(Regex("^[A-Z.]+$"))

        val isTypeValid = types.any { it.equals(orderRequest.type, ignoreCase = true) }
        val isSideValid = sides.any { it.equals(orderRequest.side, ignoreCase = true) }
        val isTimeInForceValid = timeInForces.any { it.equals(orderRequest.timeInForce, ignoreCase = true) }

        val hasQuantity = orderRequest.quantity?.toDoubleOrNull()?.let { it > 0 } ?: false
        val hasNotional = orderRequest.notional?.toDoubleOrNull()?.let { it > 0 } ?: false
        val hasValidAmount = hasQuantity || hasNotional

        val isLimitPriceValid = orderRequest.limitPrice?.toDoubleOrNull()?.let { it > 0 } ?: true
        val isStopPriceValid = orderRequest.stopPrice?.toDoubleOrNull()?.let { it > 0 } ?: true
        val isTrailPriceValid = orderRequest.trailPrice?.toDoubleOrNull()?.let { it > 0 } ?: true
        val isTrailPercentValid = orderRequest.trailPercent?.toDoubleOrNull()?.let { it > 0 } ?: true

        val areLegsValid = orderRequest.legs?.isNotEmpty() ?: true

        return isTypeValid &&
            isSideValid &&
            isTimeInForceValid &&
            isSymbolValid &&
            hasValidAmount &&
            isLimitPriceValid &&
            isStopPriceValid &&
            isTrailPriceValid &&
            isTrailPercentValid &&
            areLegsValid
    }

    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse {
        return mAlpacaRepo.createOrder(orderRequest)
    }

    suspend fun getStockData(stockAggregationRequest: StockAggregationRequest): HttpResponse {
        return mAlpacaRepo.getHistoricalData(stockAggregationRequest)
    }

    suspend fun fetchAccountDetails(): HttpResponse {
        return mAlpacaRepo.getAccountDetails()
    }

    suspend fun getOpeningHours(): HttpResponse {
        return mAlpacaRepo.getMarketOpeningHours()
    }

    suspend fun doBacktesting(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest): Result<Any, TradingLogicError> {
        return mTradingBot.backtest(strategySelector, stockAggregationRequest)
    }
    fun startBot() {
        mTradingBot.run()
    }

    fun stopBot() {
        mTradingBot.stop()
    }
}
