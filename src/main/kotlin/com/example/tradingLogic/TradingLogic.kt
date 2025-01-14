package com.example.tradingLogic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.finance.datamodel.*

class TradingLogic {

    private val mAlpacaClient = AlpacaAPI()
    private var mSymbol = ""

    private var mHistoricalBars = mutableListOf<StockBar>()
    private var mHistoricalRequest = StockAggregationRequest(symbols = mSymbol)
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

    fun setOrderParameter(orderRequest: OrderRequest) :Boolean {
        if(!areValidOrderParameter())
            return false
        mOrderRequest = orderRequest
        return true
    }

    private fun areValidOrderParameter() : Boolean {
        return mOrderRequest.type in types &&
                mOrderRequest.side in sides &&
                mOrderRequest.timeInForce in timeInForces

    }

    suspend fun createOrder(): OrderResponse? {
        return withContext(Dispatchers.IO) {
            val orderResponse = mAlpacaClient.createOrder(mOrderRequest)
            try {
                requireNotNull(orderResponse) // Ensure orderResponse is not null
            } catch(e: IllegalArgumentException) {
                println(e.message)
                return@withContext null
            }
        }
    }

    suspend fun getHistoricalBars() {
        withContext(Dispatchers.IO) {
            val historicalBars = mAlpacaClient.getHistoricalData(mHistoricalRequest)
            try {
                requireNotNull(historicalBars) // Ensure orderResponse is not null
            } catch(e: IllegalArgumentException) {
                println(e.message)
                return@withContext null
            }
        }
    }


}