package com.example.tradinglogic.strategies

import com.example.tradinglogic.IndicatorSnapshot

class NoOpStrategy : TradingStrategy {
    override fun executeAlgorithm(indicatorSnapshot: IndicatorSnapshot): TradingSignal {
        return TradingSignal.Hold
    }

}