package com.example.tradingLogic.strategies

import com.example.tradingLogic.IndicatorSnapshot


interface TradingStrategy {
    fun executeAlgorithm(indicatorSnapshot: IndicatorSnapshot): TradingSignal
}

enum class TradingSignal {
    Buy,
    Sell,
    Hold
}

class StrategyFactory {
    fun createStrategy(strategy: Strategies): TradingStrategy {
        return when (strategy) {
            Strategies.MovingAverage -> MovingAverageStrategy()
            Strategies.None -> NoOpStrategy()
        }
    }
}

enum class Strategies {
    MovingAverage,
    None    // Do Nothing
}