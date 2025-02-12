package com.example.tradingLogic.strategies

class NoOpStrategy : TradingStrategy {
    override fun executeAlgorithm(): TradingSignal {
        return TradingSignal.HOLD
    }
}