package com.example.tradinglogic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.example.finance.datamodel.*

class TradingLogic {

    private val alpacaAPIClient = AlpacaAPI()

    private var historicalBars = mutableListOf<StockBar>()

    private var mOrderRequest = OrderRequest(
        side = "",
        orderType = "",
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

    fun fetchAccountDetails(): Account {
        return runBlocking {
            val accountDetails = alpacaAPIClient.getAccountDetails()
            requireNotNull(accountDetails)
            accountDetails
        }
    }

    fun setOrderParameter(orderRequest: OrderRequest) {
        mOrderRequest = orderRequest
    }

    fun setOrderParameter(
        symbols: String,
        quantity: String,
        side: String,
        timeInForce: String,
        orderType: String
    ) {
        mOrderRequest.symbol = symbols
        mOrderRequest.quantity = quantity.toString()
        mOrderRequest.side = side
        mOrderRequest.timeInForce = timeInForce
        mOrderRequest.orderType = orderType
    }

    suspend fun createOrder(): OrderResponse {
        return withContext(Dispatchers.IO) {
            val orderResponse = alpacaAPIClient.createOrder(
                mOrderRequest.symbol, mOrderRequest.quantity, mOrderRequest.side,
                mOrderRequest.timeInForce, mOrderRequest.orderType, 0.0, 0.0
            )
            requireNotNull(orderResponse) // Ensure orderResponse is not null
        }
    }


}