package com.example.tradingLogic

import com.example.data.singleModels.StockAggregationRequest
import com.example.tradingLogic.strategies.Strategies
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val symbols: String = "",
    val positionSize: Double = 0.0,
    val timeFrame: String = "",
    val startDate: String = "",
    val strategySelection: Strategies = Strategies.None,
)

@Serializable
data class BacktestConfig(
    val strategySelector: Strategies = Strategies.MovingAverage,
    val stockAggregationRequest: StockAggregationRequest = StockAggregationRequest()
)

@Serializable
data class BacktestResult(
    val strategyName: Strategies = Strategies.None,
    val finalBalance: Double = 0.0,
    val winRate: Double = 0.0,
    val positions: Int = 0
)