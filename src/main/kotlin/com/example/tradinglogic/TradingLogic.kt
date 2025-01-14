package com.example.tradinglogic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        }
    }

    fun setOrderParameter(orderRequest: OrderRequest) {
        mOrderRequest = orderRequest
        mSymbol = orderRequest.symbol
    }

    fun setOrderParameter(
        symbol: String,
        quantity: String,
        side: String,
        timeInForce: String,
        orderType: String
    ) {
        mOrderRequest.symbol = symbol
        mSymbol = symbol
        mOrderRequest.quantity = quantity
        mOrderRequest.side = side
        mOrderRequest.timeInForce = timeInForce
        mOrderRequest.orderType = orderType
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