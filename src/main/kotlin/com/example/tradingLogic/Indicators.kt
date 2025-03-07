package com.example.tradingLogic

import com.example.data.singleModels.StockBar
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.pow

data class IndicatorSnapshot(
    val originalPrice: Double,
    val resistance: Double,
    val support: Double,
    val averageBollingerBand: Double,
    val lowerBollingerBand: Double,
    val upperBollingerBand: Double,
    val shortSMA: Double,
    val longSMA: Double,
    val rsi: Double
)

class Indicators {

    var mStock = ""

    var mOriginalPrices = mutableListOf<Double>()
        private set

    var mResistances = mutableListOf<Double>()
        private set
    var mSupports = mutableListOf<Double>()
        private set

    var mAverageBollingerBand = mutableListOf<Double>()
        private set
    var mLowerBollingerBand = mutableListOf<Double>()
        private set
    var mUpperBollingerBand = mutableListOf<Double>()
        private set

    var mShortSMA = mutableListOf<Double>()
        private set
    var mLongSMA = mutableListOf<Double>()
        private set
    var mRsi = mutableListOf<Double>()
        private set

    fun updateIndicators(historicalBars: List<StockBar>) {
        val closingPrices: List<Double> = historicalBars.map { it.close }
        mOriginalPrices = closingPrices.toMutableList()
        // TODO: refactoring
        calculateSupportLevels(closingPrices)
        calculateResistanceLevels(closingPrices)
        calculateBollingerBands(closingPrices)
        calculateRsi(closingPrices)
        calculateShortSMA(closingPrices)
        calculateLongSMA(closingPrices)
        // trim all to shortest
        trimListsToShortest()
        println(mStock)
        println(mOriginalPrices)
        println(mOriginalPrices.size)
        println(mResistances)
        println(mResistances.size)
        println(mSupports)
        println(mSupports.size)
        println(mShortSMA)
        println(mShortSMA.size)
        println(mLongSMA)
        println(mLongSMA.size)
        println(mAverageBollingerBand)
        println(mAverageBollingerBand.size)
        println(mLowerBollingerBand)
        println(mLowerBollingerBand.size)
        println(mUpperBollingerBand)
        println(mUpperBollingerBand.size)
        println(mRsi)
        println(mRsi.size)
        println("H")
        // TODO: Check calculation properly
    }

    private fun calculateSupportLevels(prices: List<Double>) {
        mSupports.clear()
        var lastSupport = prices.first()
        for (i in 1 until prices.lastIndex) {
            if (prices[i] < prices[i - 1] && prices[i] < prices[i + 1]) {
                lastSupport = prices[i]
            }
            mSupports.add(lastSupport)
        }
    }

    private fun calculateResistanceLevels(prices: List<Double>) {
        mResistances.clear()
        var lastResistance = prices.first()
        for (i in 1 until prices.lastIndex) {
            if (prices[i] > prices[i - 1] && prices[i] > prices[i + 1]) {
                lastResistance = prices[i]
            }
            mResistances.add(lastResistance) // Local maximum (resistance)
        }
    }

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
            val roundedRsi = df.format(rsi).toDouble()

            mRsi.add(roundedRsi)
        }
    }

    // clears the lists, if prices.size < window
    private fun calculateBollingerBands(prices: List<Double>, period: Int = 20, stdDevMultiplier: Double = 2.0) {
        mAverageBollingerBand.clear()
        mUpperBollingerBand.clear()
        mLowerBollingerBand.clear()
        for (i in period until prices.size) {
            if (i >= period - 1) {
                val window = prices.subList(i - period + 1, i + 1)

                val sma = window.average()
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val roundedSma = df.format(sma).toDouble()
                mAverageBollingerBand.add(roundedSma)

                val stdDev = kotlin.math.sqrt(window.sumOf { (it - sma).pow(2) } / period)

                val lowerRounded = df.format(roundedSma - stdDevMultiplier * stdDev).toDouble()
                val upperRounded = df.format(roundedSma + stdDevMultiplier * stdDev).toDouble()
                mUpperBollingerBand.add(upperRounded)
                mLowerBollingerBand.add(lowerRounded)
            }
        }
    }

    private fun trimListsToShortest() {
        val allLists = listOf(
            mLowerBollingerBand,
            mUpperBollingerBand,
            mAverageBollingerBand,
            mOriginalPrices,
            mRsi,
            mSupports,
            mResistances,
            mShortSMA,
            mLongSMA
        )
        val minSize = allLists.minOf { it.size }
        allLists.forEach { list ->
            while (list.size > minSize) {
                list.subList(0, list.size - minSize).clear() // Remove from the front
            }
        }
    }

    private fun calculateShortSMA(prices: List<Double>, period: Int = 20) {
        mShortSMA.clear()
        for (i in period until prices.size) {
            if (i >= period - 1) {
                val window = prices.subList(i - period + 1, i + 1)

                val sma = window.average()
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val roundedSma = df.format(sma).toDouble()
                mShortSMA.add(roundedSma)
            }
        }
    }

    private fun calculateLongSMA(prices: List<Double>, period: Int = 50) {
        mLongSMA.clear()
        for (i in period until prices.size) {
            if (i >= period - 1) {
                val window = prices.subList(i - period + 1, i + 1)

                val sma = window.average()
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val roundedSma = df.format(sma).toDouble()
                mLongSMA.add(roundedSma)
            }
        }
    }

    private fun getValue(list: List<Double>, index: Int?): Double {
        return if (list.isNotEmpty()) {
            index?.takeIf { it in list.indices }?.let { list[it] } ?: list.last()
        } else {
            0.0
        }
    }

    fun getIndicatorPoints(index: Int? = null): IndicatorSnapshot {
        return IndicatorSnapshot(
            originalPrice = getValue(mOriginalPrices, index),
            resistance = getValue(mResistances, index),
            support = getValue(mSupports, index),
            averageBollingerBand = getValue(mAverageBollingerBand, index),
            lowerBollingerBand = getValue(mLowerBollingerBand, index),
            upperBollingerBand = getValue(mUpperBollingerBand, index),
            shortSMA = getValue(mShortSMA, index),
            longSMA = getValue(mLongSMA, index),
            rsi = getValue(mRsi, index)
        )
    }

}