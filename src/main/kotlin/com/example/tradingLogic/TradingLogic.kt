package com.example.tradingLogic

import com.example.data.AlpacaAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.finance.datamodel.*

class TradingLogic {

    private val mAlpacaClient = AlpacaAPI()

    private var mHistoricalBars = mutableListOf<StockBar>()
    private var mHistoricalRequest = StockAggregationRequest()
    private var mOrderRequest = OrderRequest()

    /************************************************************
    Methods
     ************************************************************/
    suspend fun fetchAccountDetails(): Account {
        return withContext(Dispatchers.IO) {
            val accountDetails = mAlpacaClient.getAccountDetails()
            requireNotNull(accountDetails)
            accountDetails
        }
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

    suspend fun createOrder(): ApiResponse? {
        return withContext(Dispatchers.IO) {
            val orderResponse = mAlpacaClient.createOrder(mOrderRequest)
            try {
                requireNotNull(orderResponse) // Ensure orderResponse is not null
            } catch (e: IllegalArgumentException) {
                println(e.message)
                return@withContext null
            }
        }
    }

    suspend fun getHistoricalBars(): StockAggregationResponse? {
        return withContext(Dispatchers.IO) {
            val historicalBars = mAlpacaClient.getHistoricalData(mHistoricalRequest)
            try {
                requireNotNull(historicalBars) // Ensure orderResponse is not null
            } catch (e: IllegalArgumentException) {
                println(e.message)
                return@withContext null
            }
        }
    }

}