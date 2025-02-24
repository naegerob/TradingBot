package com.example.tradingLogic

import com.example.tradingLogic.strategies.Strategies

data class BotConfig(
    val symbols: String = "",
    val positionSize: Double = 0.0,
    val timeFrame: String = "",
    val startDate: String = "",
    val strategySelection: Strategies = Strategies.None,
)

data class BacktestResult(
    val strategyName: Strategies = Strategies.None,
    val totalProfit: Double = 0.0,
    val avgProfit: Double = 0.0,
    val winRate: Double = 0.0,
)