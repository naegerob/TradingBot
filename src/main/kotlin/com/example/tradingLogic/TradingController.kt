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
        return orderRequest.type in types &&
                orderRequest.side in sides &&
                orderRequest.timeInForce in timeInForces
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