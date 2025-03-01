package com.example.tradingLogic.strategies

import com.example.tradingLogic.IndicatorSnapshot

class NoOpStrategy : TradingStrategy {
    override fun executeAlgorithm(indicatorSnapshot: IndicatorSnapshot): TradingSignal {
        return TradingSignal.Hold
    }

}