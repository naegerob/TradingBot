package com.example.tradingLogic.strategies

import com.example.tradingLogic.Indicators

class MovingAverageStrategy : TradingStrategy {

    override fun executeAlgorithm(indicators: Indicators): TradingSignal {
        val shortSMA = indicators.mShortSMA
        val longSMA = indicators.mLongSMA
        if(shortSMA.first() > longSMA.first()) {
            println(shortSMA.first())
            println(longSMA.first())
            return TradingSignal.Buy
        } else if (shortSMA.first() < longSMA.first()) {
            return TradingSignal.Sell
        }
        return TradingSignal.Hold
    }

    override fun backTestAlgorithm(indicators: Indicators): List<TradingSignal> {
        val shortSMA = indicators.mShortSMA
        val longSMA = indicators.mLongSMA
        println("indicators.mLongSMA")
        println(indicators.mLongSMA)
        println(indicators.mShortSMA)
        val signalList = mutableListOf<TradingSignal>()
        indicators.mLongSMA.forEachIndexed { index, _ ->
            if(shortSMA[index] > longSMA[index]) {
                signalList.add(TradingSignal.Buy)
            } else if (shortSMA[index] < longSMA[index]) {
                signalList.add(TradingSignal.Sell)
            } else {
                signalList.add(TradingSignal.Hold)
            }
        }
        return signalList

    }

}