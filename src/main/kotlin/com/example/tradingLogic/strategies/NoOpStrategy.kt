package com.example.tradingLogic.strategies

import com.example.tradingLogic.Indicators

class NoOpStrategy : TradingStrategy {
    override fun executeAlgorithm(indicators: Indicators): TradingSignal {
        return TradingSignal.Hold
    }
}