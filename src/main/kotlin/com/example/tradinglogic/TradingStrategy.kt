package com.example.tradinglogic


/**
 * This table is implemented in TradingBot
 * Current position     Signal	Action
 * FLAT                 BUY	    Open LONG
 * FLAT	                SELL	Open SHORT
 * FLAT                 HOLD	Do nothing
 * LONG                 HOLD	Do nothing
 * LONG                 SELL	Close LONG
 * LONG                 BUY     Do nothing
 * SHORT                HOLD	Do nothing
 * SHORT	            BUY     Close SHORT
 * SHORT	            SELL	Do nothing
 */

enum class TradingPosition {
    Long,
    Short,
    Flat
}
enum class TradingSignal {
    Buy,
    Sell,
    Hold
}
enum class TradingAction {
    OpenLong,
    OpenShort,
    CloseLong,
    CloseShort,
    DoNothing
}

typealias TradingStrategy = (IndicatorSnapshot) -> TradingSignal

val MovingAverageStrategy : TradingStrategy = {
    when {
        it.shortSMA > it.longSMA    -> TradingSignal.Buy
        it.shortSMA < it.longSMA    -> TradingSignal.Sell
        else                        -> TradingSignal.Hold
    }
}

val NoOpStrategy : TradingStrategy = {
    TradingSignal.Hold
}

class StrategyFactory {
    fun createStrategy(strategy: Strategies): TradingStrategy {
        return when (strategy) {
            Strategies.MovingAverage -> MovingAverageStrategy
            Strategies.None -> NoOpStrategy
        }
    }
}

enum class Strategies {
    MovingAverage,
    None    // Do Nothing
}