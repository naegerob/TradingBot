package com.example.tradingLogic.strategies

interface TradingStrategy {
    fun executeAlgorithm() : TradingSignal
}
enum class TradingSignal {
    BUY,
    SELL,
    HOLD
}

class StrategyFactory {
    fun createStrategy(strategy: Strategies): TradingStrategy {
        return when (strategy) {
            Strategies.movingAverage    -> MovingAverageStrategy()
            Strategies.none             -> NoOpStrategy()
        }
    }
}

enum class Strategies {
    movingAverage,
    none    // Do Nothing
}