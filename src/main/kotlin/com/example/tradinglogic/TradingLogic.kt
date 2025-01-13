package com.example.tradingLogic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.finance.datamodel.*

class TradingLogic {

    private val alpacaAPIClient = AlpacaAPI()

    private var historicalBars = mutableListOf<StockBar>()

    private var mOrderRequest = OrderRequest(
        side = "",
        type = "",
        timeInForce = "",
        quantity = "",
        symbol = "",
        limitPrice = null,
        stopPrice = null,
        trailPrice = null,
        trailPercent = null,
        extendedHours = false,
        clientOrderId = null,
        orderClass = "",
        takeProfit = null,
        stopLoss = null,
        positionIntent = null
    )

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

    suspend fun fetchAccountDetails(): Account {
        return withContext(Dispatchers.IO) {
            val accountDetails = alpacaAPIClient.getAccountDetails()
            requireNotNull(accountDetails)
        }
    }

    suspend fun createOrder(): OrderResponse {
        return withContext(Dispatchers.IO) {
            val orderResponse = alpacaAPIClient.createOrder(mOrderRequest)
            requireNotNull(orderResponse)
        }
    }
}