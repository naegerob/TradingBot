package com.example.tradingLogic.strategies

import com.example.tradingLogic.IndicatorSnapshot

class MovingAverageStrategy : TradingStrategy {

    override fun executeAlgorithm(indicatorSnapshot: IndicatorSnapshot): TradingSignal {
        // TODO: Consider only buying if difference increases certain level
        if (indicatorSnapshot.shortSMA > indicatorSnapshot.longSMA) {
            return TradingSignal.Buy
        } else if (indicatorSnapshot.shortSMA < indicatorSnapshot.longSMA) {
            return TradingSignal.Sell
        }
        return TradingSignal.Hold
    }

}