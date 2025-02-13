package com.example.tradingLogic.strategies

import com.example.tradingLogic.Indicators

class MovingAverageStrategy : TradingStrategy {

    override fun executeAlgorithm(indicators: Indicators): TradingSignal {
        val shortSMA = indicators.mShortSMA
        val longSMA = indicators.mLongSMA
        if(shortSMA.last() > longSMA.last()) {
            return TradingSignal.BUY
        } else if (shortSMA.last() < longSMA.last()) {
            return TradingSignal.SELL
        }
        return TradingSignal.HOLD
    }

}