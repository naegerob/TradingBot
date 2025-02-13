package com.example.tradingLogic

import com.example.data.*
import com.example.data.singleModels.*
import com.example.tradingLogic.strategies.Strategies
import com.example.tradingLogic.strategies.StrategyFactory
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

class TradingController {

    private val mAlpacaClient = AlpacaRepository()

    private var mHistoricalRequest = StockAggregationRequest()
    private var mOrderRequest = OrderRequest()
    private var mHistoricalBars = listOf<StockBar>()

    var mIndicators = Indicators()
        private set
    private val mStrategySelector: Strategies = Strategies.none
    // TODO: Consider using builder pattern
    private val mTradingBot = TradingBot(
        mHistoricalRequest,
        StrategyFactory().createStrategy(mStrategySelector),
        mIndicators,
        mAlpacaClient)

    /************************************************************
    Methods
     ************************************************************/
    private fun areValidStockRequestParameter(stockAggregationRequest: StockAggregationRequest): Boolean {
        val isSymbolValid = stockAggregationRequest.symbols.isNotEmpty()
        val isTimeframeValid = timeframes.any { stockAggregationRequest.timeframe.contains(it) }
        val isFeedValid = feeds.any { stockAggregationRequest.feed.contains(it) }
        val isSortValid = sorts.any { stockAggregationRequest.sort.contains(it) }
        val hasNumberInTimeFrame = stockAggregationRequest.timeframe.contains(Regex("\\d"))
        return isSymbolValid && isTimeframeValid && isFeedValid && isSortValid && hasNumberInTimeFrame
    }

    private fun areValidOrderParameter(orderRequest: OrderRequest): Boolean {
        return orderRequest.type in types &&
                orderRequest.side in sides &&
                orderRequest.timeInForce in timeInForces
    }

    private fun setAndGetFirstSymbol(): String {
        mIndicators.mStock = mHistoricalRequest.symbols.substringBefore(",")
        return mIndicators.mStock
    }


    fun setOrderParameter(orderRequest: OrderRequest): Boolean {
        if (!areValidOrderParameter(orderRequest))
            return false
        mOrderRequest = orderRequest
        return true
    }

    fun setAggregationParameter(stockAggregationRequest: StockAggregationRequest): Boolean {
        if (!areValidStockRequestParameter(stockAggregationRequest)) {
            return false
        }
        mHistoricalRequest = stockAggregationRequest
        return true
    }

    suspend fun createOrder(): HttpResponse {
        return mAlpacaClient.createOrder(mOrderRequest)
    }

    suspend fun storeStockData(): HttpResponse {
        val httpResponse = mAlpacaClient.getHistoricalData(mHistoricalRequest)
        try {
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val stockResponse = httpResponse.body<StockAggregationResponse>()
                    mHistoricalBars = stockResponse.bars[setAndGetFirstSymbol()]!!
                    mIndicators.updateIndicators(mHistoricalBars) // TODO: This should be moved and called from the strategy. As User we should only read the indicators, but not set manually
                }
            }
        } catch (e: Exception) {
            // TODO: Error Handling
            println(e)
            return httpResponse
        }
        return httpResponse
    }

    suspend fun fetchAccountDetails(): HttpResponse {
        return mAlpacaClient.getAccountDetails()
    }

    fun startBot() {
        mTradingBot.mIsRunning = true
    }

    fun stopBot() {
        mTradingBot.mIsRunning = false
    }
}