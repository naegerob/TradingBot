package com.example.tradingLogic

import com.example.data.*
import io.ktor.client.statement.*

class TradingLogic {

    private val mAlpacaClient = AlpacaAPI()

    private var mHistoricalBars = mutableListOf<StockBar>()
    private var mHistoricalRequest = StockAggregationRequest()
    private var mOrderRequest = OrderRequest()
        get


    /************************************************************
    Methods
     ************************************************************/
    private fun areValidStockRequestParameter(): Boolean {
        val isTimeframeValid = timeframes.any { mHistoricalRequest.timeframe.contains(it) }
        val isFeedValid = feeds.any { mHistoricalRequest.feed.contains(it) }
        val isSortValid = sorts.any { mHistoricalRequest.sort.contains(it) }
        val hasNumberInTimeFrame = mHistoricalRequest.timeframe.contains(Regex("\\d"))
        return isTimeframeValid && isFeedValid && isSortValid && hasNumberInTimeFrame
    }

    private fun areValidOrderParameter(): Boolean {
        return mOrderRequest.type in types &&
                mOrderRequest.side in sides &&
                mOrderRequest.timeInForce in timeInForces

    }

    fun setOrderParameter(orderRequest: OrderRequest): Boolean {
        if (!areValidOrderParameter())
            return false
        mOrderRequest = orderRequest
        return true
    }

    fun setAggregationParameter(stockAggregationRequest: StockAggregationRequest): Boolean {
        if (!areValidStockRequestParameter()) {
            return false
        }
        mHistoricalRequest = stockAggregationRequest
        return true
    }


    suspend fun createOrder(): HttpResponse {
        return mAlpacaClient.createOrder(mOrderRequest)
    }

    suspend fun getHistoricalBars(): HttpResponse {
        return mAlpacaClient.getHistoricalData(mHistoricalRequest)
    }

    suspend fun fetchAccountDetails(): HttpResponse {
        return mAlpacaClient.getAccountDetails()
    }

}