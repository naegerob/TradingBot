package com.example.tradinglogic

import com.example.data.alpaca.ApiResponse
import com.example.data.singleModels.StockAggregationRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TakeProfit(
    @SerialName("limit_price") val limitPrice: String = ""
)

@Serializable
data class StopLoss(
    @SerialName("stop_price") val stopPrice: String = "",
    @SerialName("limit_price") val limitPrice: String = ""
)

@Serializable
data class BotConfig(
    @SerialName("symbols") val symbols: String = "",
    @SerialName("position_size") val positionSize: Double = 0.0,
    @SerialName("time_frame") val timeframe: String = "",
    @SerialName("number_samples") val numberSamples: Int = 0,
    @SerialName("order_class") val orderClass: String = "",
    @SerialName("order_type") val orderType: String = "",
    @SerialName("strategy_selection") val strategySelection: Strategies = Strategies.None,
    @SerialName("take_profit") val takeProfit: TakeProfit? = null,
    @SerialName("stop_loss") val stopLoss: StopLoss? = null
) : ApiResponse()

@Serializable
data class BacktestConfig(
    @SerialName("strategy_selector") val strategySelector: Strategies = Strategies.MovingAverage,
    @SerialName("stock_aggregation_request") val stockAggregationRequest: StockAggregationRequest = StockAggregationRequest()
) : ApiResponse()

@Serializable
data class BacktestResult(
    @SerialName("strategy_name") val strategyName: Strategies = Strategies.None,
    @SerialName("final_equity") val finalEquity: Double = 0.0,
    @SerialName("profit") val profit: Double = 0.0,
    @SerialName("roi_percent") val roiPercent: Double = 0.0,
    @SerialName("winRate_percent") val winRatePercent: Double = 0.0,
    @SerialName("positions") val positions: Int = 0
) : ApiResponse()