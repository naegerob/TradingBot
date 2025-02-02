package com.example.tradingLogic

import com.example.data.*
import com.example.data.singleModels.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

class TradingLogic {

    private val mAlpacaClient = AlpacaAPI()

    private var mHistoricalRequest = StockAggregationRequest()
    private var mOrderRequest = OrderRequest()
    private var mHistoricalBars = listOf<StockBar>()

    enum class Windows(val windowLength: Int) {
        SHORT(20),
        LONG(50),
        BB(40)
    }

    private var mMovingAverage = emptyMap<Windows, MutableList<Int>>()

    private var mResistance = null
    private var mSupport = null

    enum class Bands {
        LOW, MIDDLE, HIGH
    }

    private var mBollingerBands = emptyMap<Bands, MutableList<Int>>()

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

    private fun getFirstSymbol(): String {
        return mHistoricalRequest.symbols.substringBefore(",")
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
        val httpResponse = mAlpacaClient.getHistoricalData(mHistoricalRequest)
        try {
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val stockResponse = httpResponse.body<StockAggregationResponse>()
                    mHistoricalBars = stockResponse.bars[getFirstSymbol()]!!
                }
            }
        } catch (e: Exception) {
            // TODO: Error Handling
            return httpResponse
        }
        return httpResponse
    }

    suspend fun fetchAccountDetails(): HttpResponse {
        return mAlpacaClient.getAccountDetails()
    }

    fun calculateIndicators() {
        val closingPrices: List<Double> = mHistoricalBars.map { it.close }
        val meanShort = closingPrices
            .take(Windows.SHORT.windowLength)
            .average()

        val meanLong = closingPrices
            .take(Windows.LONG.windowLength)
            .average()

        val middleBollingerBand = closingPrices
            .take(Windows.BB.windowLength)
            .average()

        val upperBollingerBand = closingPrices
            .windowed(Windows.BB.windowLength)


        // TODO: calculate upper and lower BollingerBand
        // TODO: Calculate RSI
        // TODO: Check calculation properly





    }


}