package com.example.tradingLogic

import com.example.data.AlpacaRepository
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.Strategies
import io.ktor.client.statement.*

class TradingController {

    private val mAlpacaClient = AlpacaRepository()

    var mIndicators = Indicators()
        private set

    // TODO: Consider using builder pattern
    var mTradingBot = TradingBot(mAlpacaClient)
        private set

    /************************************************************
    Methods
     ************************************************************/
    fun areValidStockRequestParameter(stockAggregationRequest: StockAggregationRequest): Boolean {
        val isSymbolValid = stockAggregationRequest.symbols.isNotEmpty() && !stockAggregationRequest.symbols.contains(",")
        val isTimeframeValid = timeframes.any { stockAggregationRequest.timeframe.contains(it) }
        val isFeedValid = feeds.any { stockAggregationRequest.feed.contains(it) }
        val isSortValid = sorts.any { stockAggregationRequest.sort.contains(it) }
        val isCorrectTimeframe = stockAggregationRequest.timeframe.contains(Regex("\\d+(Min|T|Hour|H|Day|D|Week|W|Month|M)"))
        return isSymbolValid && isTimeframeValid && isFeedValid && isSortValid && isCorrectTimeframe
    }

    fun areValidOrderParameter(orderRequest: OrderRequest): Boolean {
        return orderRequest.type in types &&
                orderRequest.side in sides &&
                orderRequest.timeInForce in timeInForces
    }

    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse {
        return mAlpacaClient.createOrder(orderRequest)
    }

    suspend fun getStockData(stockAggregationRequest: StockAggregationRequest): HttpResponse {
        return mAlpacaClient.getHistoricalData(stockAggregationRequest)
    }

    suspend fun fetchAccountDetails(): HttpResponse {
        return mAlpacaClient.getAccountDetails()
    }

    suspend fun doBacktesting(strategySelector: Strategies, stockAggregationRequest: StockAggregationRequest): BacktestResult {
        return mTradingBot.backtest(strategySelector, stockAggregationRequest)
    }
    fun startBot() {
        mTradingBot.run()
    }

    fun stopBot() {
        mTradingBot.stop()
    }
}