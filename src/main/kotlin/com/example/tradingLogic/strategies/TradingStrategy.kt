package com.example.tradingLogic.strategies

import com.example.tradingLogic.Indicators

interface TradingStrategy {
    fun executeAlgorithm(indicators: Indicators) : TradingSignal
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