package com.example.tradingLogic

import com.example.data.alpaca.ApiResponse
import com.example.data.singleModels.StockAggregationRequest
import com.example.tradingLogic.strategies.Strategies
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    @SerialName("symbols") val symbols: String = "",
    @SerialName("position_size") val positionSize: Double = 0.0,
    @SerialName("time_frame") val timeFrame: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("strategy_selection") val strategySelection: Strategies = Strategies.None,
) : ApiResponse()

@Serializable
data class BacktestConfig(
    @SerialName("strategy_selector") val strategySelector: Strategies = Strategies.MovingAverage,
    @SerialName("stock_aggregation_request") val stockAggregationRequest: StockAggregationRequest = StockAggregationRequest()
) : ApiResponse()

@Serializable
data class BacktestResult(
    @SerialName("strategy_name") val strategyName: Strategies = Strategies.None,
    @SerialName("final_balance") val finalBalance: Double = 0.0,
    @SerialName("roi_percent") val roiPercent: Double = 0.0,
    @SerialName("winRate_percent") val winRatePercent: Double = 0.0,
    @SerialName("positions") val positions: Int = 0
) : ApiResponse()