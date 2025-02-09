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

    private var mResistance = 0.0
    private var mSupport = 0.0

    private var mAverageBolligerBand = mutableListOf<Double>()
    private var mLowerBollingerBand = mutableListOf<Double>()
    private var mUpperBollingerBand = mutableListOf<Double>()

    private var mRsi = mutableListOf<Double>()

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

    private fun getFirstSymbol(): String {
        return mHistoricalRequest.symbols.substringBefore(",")
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
            println(e)
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
        calculateSupportResistance(closingPrices)
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
        println(mSupport)
        println(mResistance)
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
        var averageGain = gains.average()
        var averageLoss = losses.average()

        // Compute RSI using exponential smoothing
        for (i in period until prices.size) {
            val delta = prices[i] - prices[i - 1]
            val gain = if (delta > 0) delta else 0.0
            val loss = if (delta < 0) -delta else 0.0

            // Smoothed averages
            averageGain = ((averageGain * (period - 1)) + gain) / period
            averageLoss = ((averageLoss * (period - 1)) + loss) / period

            val rs = if (averageLoss == 0.0) Double.POSITIVE_INFINITY else averageGain / averageLoss
            val rsi = 100 - (100 / (1 + rs))
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            val roundedRsi =  df.format(rsi).toDouble()

            mRsi.add(roundedRsi)
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
        for (i in period until sortedPrices.size) {
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
            }
        }
    }

    private fun calculateSupportResistance(prices: List<Double>, window: Int = 5) {
        for (i in window until prices.size - window) {
            val subList = prices.subList(i - window, i + window + 1)

            val minValue = subList.minOrNull() ?: prices[i]
            val maxValue = subList.maxOrNull() ?: prices[i]

            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING

            if (prices[i] == minValue) {
                mSupport =  df.format(minValue).toDouble()
            }
            if (prices[i] == maxValue) {
                mResistance = df.format(maxValue).toDouble()
            }
        }
    }



}