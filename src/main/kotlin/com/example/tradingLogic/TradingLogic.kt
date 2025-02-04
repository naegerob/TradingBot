package com.example.tradingLogic

import com.example.data.*
import com.example.data.singleModels.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.pow

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

    private var mRsi = mutableListOf<Double>()

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

    private fun calculateIndicators() {
        val closingPrices: List<Double> = mHistoricalBars.map { it.close }

        calculateBollingerBands(closingPrices)
        calculateRsi(closingPrices)
        println(closingPrices)
        println(closingPrices.size)
        println(mAverageBolligerBand)
        println(mAverageBolligerBand.size)
        println(mLowerBollingerBand)
        println(mLowerBollingerBand.size)
        println(mUpperBollingerBand)
        println(mUpperBollingerBand.size)
        println(mRsi)
        println(mRsi.size)
        println("H")
        // TODO: Calculate RSI
        // TODO: Check calculation properly

    }

    // TODO: why is it 14 and sometimes 13?
    private fun calculateRsi(prices: List<Double>, period: Int = 14) {

        mRsi.clear()
        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()

        // Calculate initial gains and losses
        for (i in 1 until period) {
            val delta = prices[i] - prices[i - 1]
            if (delta > 0) {
                gains.add(delta)
            } else {
                losses.add(-delta)
            }
        }

        // Compute first average gain and loss
        var avgGain = gains.average()
        var avgLoss = losses.average()

        // Compute RSI using exponential smoothing
        for (i in period until prices.size) {
            val delta = prices[i] - prices[i - 1]
            val gain = if (delta > 0) delta else 0.0
            val loss = if (delta < 0) -delta else 0.0

            // Smoothed averages
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period

            val rs = if (avgLoss == 0.0) Double.POSITIVE_INFINITY else avgGain / avgLoss
            val rsi = 100 - (100 / (1 + rs))

            mRsi[i] = rsi
        }
    }

    // clears the lists, if prices.size < window
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

                val sma = window.average()
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val roundedSma =  df.format(sma).toDouble()
                mAverageBolligerBand.add(roundedSma)

                val stdDev = kotlin.math.sqrt(window.sumOf { (it - sma).pow(2) } / period)

                val lowerRounded = df.format(roundedSma - stdDevMultiplier * stdDev).toDouble()
                val upperRounded = df.format(roundedSma + stdDevMultiplier * stdDev).toDouble()
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