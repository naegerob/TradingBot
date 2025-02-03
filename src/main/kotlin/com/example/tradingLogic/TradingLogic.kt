package com.example.tradingLogic

import com.example.data.*
import com.example.data.singleModels.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.roundToLong

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

    private var mAverageBolligerBand = mutableListOf<Double>()
    private var mLowerBollingerBand = mutableListOf<Double>()
    private var mUpperBollingerBand = mutableListOf<Double>()

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
                    calculateIndicators()
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

        calculateBollingerBands(closingPrices)
        println(closingPrices)
        println(closingPrices.size)
        println(mAverageBolligerBand)
        println(mAverageBolligerBand.size)
        println(mLowerBollingerBand)
        println(mLowerBollingerBand.size)
        println(mUpperBollingerBand)
        println(mUpperBollingerBand.size)
        println("H")
        // TODO: Calculate RSI
        // TODO: Check calculation properly

    }

    private fun calculateBollingerBands(prices: List<Double>, period: Int = 20, stdDevMultiplier: Double = 2.0) {
        var sortedPrices = prices
        if(mHistoricalRequest.sort == sort[1]) {
            sortedPrices = prices.reversed()
        }
        mAverageBolligerBand.clear()
        mUpperBollingerBand.clear()
        mLowerBollingerBand.clear()
        for (i in sortedPrices.indices) {
            if (i >= period - 1) {
                val window = sortedPrices.subList(i - period + 1, i + 1)

                // Calculate SMA
                val sma = window.average()
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val roundedAverage =  df.format(sma).toDouble()
                mAverageBolligerBand.add(roundedAverage)

                // Calculate Standard Deviation
                val stdDev = kotlin.math.sqrt(window.sumOf { (it - sma) * (it - sma) } / period)

                // Calculate Upper and Lower Bands
                val lowerRounded = df.format(sma - stdDevMultiplier * stdDev).toDouble()
                val upperRounded = df.format(sma + stdDevMultiplier * stdDev).toDouble()
                mUpperBollingerBand.add(upperRounded)
                mLowerBollingerBand.add(lowerRounded)
            } else {
                mAverageBolligerBand.add(0.0)  // Fill with 0 until enough data points
                mUpperBollingerBand.add(0.0)
                mLowerBollingerBand.add(0.0)
            }
        }
    }



}