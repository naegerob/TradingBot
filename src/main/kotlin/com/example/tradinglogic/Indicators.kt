package com.example.tradinglogic

import com.example.data.singleModels.StockBar
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

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

    private val _originalPrices = mutableListOf<Double>()
    val mOriginalPrices: List<Double> get() = _originalPrices

    private val _resistances = mutableListOf<Double>()
    val mResistances: List<Double> get() = _resistances

    private val _supports = mutableListOf<Double>()
    val mSupports: List<Double> get() = _supports

    private val _averageBollingerBand = mutableListOf<Double>()
    val mAverageBollingerBand: List<Double> get() = _averageBollingerBand

    private val _lowerBollingerBand = mutableListOf<Double>()
    val mLowerBollingerBand: List<Double> get() = _lowerBollingerBand

    private val _upperBollingerBand = mutableListOf<Double>()
    val mUpperBollingerBand: List<Double> get() = _upperBollingerBand

    private val _shortSMA = mutableListOf<Double>()
    val mShortSMA: List<Double> get() = _shortSMA

    private val _longSMA = mutableListOf<Double>()
    val mLongSMA: List<Double> get() = _longSMA

    private val _rsi = mutableListOf<Double>()
    val mRsi: List<Double> get() = _rsi

    fun updateIndicators(historicalBars: List<StockBar>): Result<Any, TradingLogicError> {
        val closingPrices: List<Double> = historicalBars.map { it.close }

        _originalPrices.clear()
        _originalPrices.addAll(closingPrices)

        calculateSupportLevels(closingPrices)
        calculateResistanceLevels(closingPrices)
        calculateBollingerBands(closingPrices)
        calculateRsi(closingPrices)
        calculateShortSMA(closingPrices)
        calculateLongSMA(closingPrices)

        // trim all to shortest
        return when (val result = trimListsToShortest()) {
            is Result.Error -> Result.Error(result.error)
            is Result.Success -> Result.Success(Unit)
        }
    }

    private fun calculateSupportLevels(prices: List<Double>) {
        _supports.clear()
        var lastSupport = prices.first()
        for (i in 1 until prices.lastIndex) {
            if (prices[i] < prices[i - 1] && prices[i] < prices[i + 1]) {
                lastSupport = prices[i]
            }
            _supports.add(lastSupport)
        }
    }

    private fun calculateResistanceLevels(prices: List<Double>) {
        _resistances.clear()
        var lastResistance = prices.first()
        for (i in 1 until prices.lastIndex) {
            if (prices[i] > prices[i - 1] && prices[i] > prices[i + 1]) {
                lastResistance = prices[i]
            }
            _resistances.add(lastResistance)
        }
    }

    private fun calculateRsi(prices: List<Double>, period: Int = 14) {
        var tempPeriod = period
        _rsi.clear()

        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()
        if (prices.size < tempPeriod) {
            tempPeriod = prices.size
        }

        // Calculate initial gains and losses
        for (i in 1 until tempPeriod) {
            val delta = prices[i] - prices[i - 1]
            if (delta > 0) gains.add(delta) else losses.add(-delta)
        }

        // Compute first average gain and loss
        var averageGain = gains.average()
        var averageLoss = losses.average()

        // Compute RSI using exponential smoothing
        for (i in tempPeriod until prices.size) {
            val delta = prices[i] - prices[i - 1]
            val gain = if (delta > 0) delta else 0.0
            val loss = if (delta < 0) -delta else 0.0

            averageGain = ((averageGain * (tempPeriod - 1)) + gain) / tempPeriod
            averageLoss = ((averageLoss * (tempPeriod - 1)) + loss) / tempPeriod

            val rs = if (averageLoss == 0.0) Double.POSITIVE_INFINITY else averageGain / averageLoss
            val rsi = 100 - (100 / (1 + rs))

            val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.CEILING }
            _rsi.add(df.format(rsi).toDouble())
        }
    }

    private fun calculateBollingerBands(prices: List<Double>, period: Int = 20, stdDevMultiplier: Double = 2.0) {
        _averageBollingerBand.clear()
        _upperBollingerBand.clear()
        _lowerBollingerBand.clear()

        val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.CEILING }

        for (i in period until prices.size) {
            if (i >= period - 1) {
                val window = prices.subList(i - period + 1, i + 1)

                val sma = window.average()
                val roundedSma = df.format(sma).toDouble()
                _averageBollingerBand.add(roundedSma)

                val stdDev = sqrt(window.sumOf { (it - sma).pow(2) } / period)
                _upperBollingerBand.add(df.format(roundedSma + stdDevMultiplier * stdDev).toDouble())
                _lowerBollingerBand.add(df.format(roundedSma - stdDevMultiplier * stdDev).toDouble())
            }
        }
    }

    private fun trimListsToShortest(): Result<Any, TradingLogicError> {
        val allLists = listOf(
            _lowerBollingerBand,
            _upperBollingerBand,
            _averageBollingerBand,
            _originalPrices,
            _rsi,
            _supports,
            _resistances,
            _shortSMA,
            _longSMA
        )
        val minSize = allLists.minOf { it.size }
        if (minSize == 0) {
            return Result.Error(TradingLogicError.DataError.TOO_LESS_DATA_SAMPLES)
        }
        allLists.forEach { list ->
            while (list.size > minSize) {
                list.subList(0, list.size - minSize).clear() // Remove from the front
            }
        }
        return Result.Success(Unit)
    }

    private fun calculateShortSMA(prices: List<Double>, period: Int = 20) {
        _shortSMA.clear()
        val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.CEILING }

        for (i in period until prices.size) {
            if (i >= period - 1) {
                val window = prices.subList(i - period + 1, i + 1)
                _shortSMA.add(df.format(window.average()).toDouble())
            }
        }
    }

    private fun calculateLongSMA(prices: List<Double>, period: Int = 50) {
        _longSMA.clear()
        val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.CEILING }

        for (i in period until prices.size) {
            if (i >= period - 1) {
                val window = prices.subList(i - period + 1, i + 1)
                _longSMA.add(df.format(window.average()).toDouble())
            }
        }
    }

    private fun getValueFromList(list: List<Double>, index: Int): Result<Double, TradingLogicError> {
        return if (list.isNotEmpty()) {
            Result.Success(index.takeIf { it in list.indices }?.let { list[it] } ?: list.last())
        } else {
            Result.Error(TradingLogicError.DataError.LIST_IS_EMPTY)
        }
    }

    fun getIndicatorPoints(index: Int): Result<IndicatorSnapshot, TradingLogicError> {
        val originalPriceResult = getValueFromList(mOriginalPrices, index)
        val resistanceResult = getValueFromList(mResistances, index)
        val supportResult = getValueFromList(mSupports, index)
        val avgBBResult = getValueFromList(mAverageBollingerBand, index)
        val lowerBBResult = getValueFromList(mLowerBollingerBand, index)
        val upperBBResult = getValueFromList(mUpperBollingerBand, index)
        val shortSMAResult = getValueFromList(mShortSMA, index)
        val longSMAResult = getValueFromList(mLongSMA, index)
        val rsiResult = getValueFromList(mRsi, index)

        val results = listOf(
            originalPriceResult,
            resistanceResult,
            supportResult,
            avgBBResult,
            lowerBBResult,
            upperBBResult,
            shortSMAResult,
            longSMAResult,
            rsiResult
        )
        results.forEach { result ->
            if (result is Result.Error) return Result.Error(result.error)
        }

        return Result.Success(
            IndicatorSnapshot(
                originalPrice = (originalPriceResult as Result.Success).data,
                resistance = (resistanceResult as Result.Success).data,
                support = (supportResult as Result.Success).data,
                averageBollingerBand = (avgBBResult as Result.Success).data,
                lowerBollingerBand = (lowerBBResult as Result.Success).data,
                upperBollingerBand = (upperBBResult as Result.Success).data,
                shortSMA = (shortSMAResult as Result.Success).data,
                longSMA = (longSMAResult as Result.Success).data,
                rsi = (rsiResult as Result.Success).data
            )
        )
    }
}