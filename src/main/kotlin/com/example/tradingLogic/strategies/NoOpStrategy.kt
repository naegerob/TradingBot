package com.example.tradingLogic.strategies

import com.example.tradingLogic.Indicators

class NoOpStrategy : TradingStrategy {
    override fun executeAlgorithm(indicators: Indicators): TradingSignal {
        return TradingSignal.Hold
    }

    override fun backTestAlgorithm(indicators: Indicators): List<TradingSignal> {
        return emptyList()
    }
}