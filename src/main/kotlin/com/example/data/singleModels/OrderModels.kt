package com.example.data.singleModels

import com.example.data.alpaca.ApiResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    @SerialName("symbol") var symbol: String = "AAPL",
    @SerialName("qty") var quantity: String? = null,
    @SerialName("notional") var notional: String? = "10",
    @SerialName("side") var side: String = "buy",
    @SerialName("type") var type: String = "market",
    @SerialName("time_in_force") var timeInForce: String = "day",
    @SerialName("limit_price") val limitPrice: String? = null,
    @SerialName("stop_price") val stopPrice: String? = null,
    @SerialName("trail_price") val trailPrice: String? = null,
    @SerialName("trail_percent") val trailPercent: String? = null,
    @SerialName("extended_hours") val extendedHours: Boolean = false,
    @SerialName("client_order_id") val clientOrderId: String? = null,
    @SerialName("order_class") val orderClass: String? = null,
    @SerialName("legs") val legs: List<OrderLeg>? = null,
    @SerialName("take_profit") val takeProfit: TakeProfit? = null,
    @SerialName("stop_loss") val stopLoss: StopLoss? = null,
    @SerialName("position_intent") val positionIntent: String? = null
) : ApiResponse()

@Serializable
data class OrderResponse(
    val id: String,
    @SerialName("client_order_id") val clientOrderId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("filled_at") val filledAt: String? = null,
    @SerialName("expired_at") val expiredAt: String? = null,
    @SerialName("canceled_at") val canceledAt: String? = null,
    @SerialName("failed_at") val failedAt: String? = null,
    @SerialName("replaced_at") val replacedAt: String? = null,
    @SerialName("replaced_by") val replacedBy: String? = null,
    val replaces: String? = null,
    @SerialName("asset_id") val assetId: String,
    val symbol: String,
    @SerialName("asset_class") val assetClass: String,
    val notional: String? = null,
    val qty: String? = null,
    @SerialName("filled_qty") val filledQty: String,
    @SerialName("filled_avg_price") val filledAvgPrice: String? = null,
    @SerialName("order_class") val orderClass: String,
    @SerialName("order_type") val orderType: String,
    val type: String,
    val side: String,
    @SerialName("time_in_force") val timeInForce: String,
    @SerialName("limit_price") val limitPrice: String? = null,
    @SerialName("stop_price") val stopPrice: String? = null,
    val status: String,
    @SerialName("extended_hours") val extendedHours: Boolean,
    @SerialName("legs") val legs: List<OrderLeg>? = null,
    @SerialName("trail_percent") val trailPercent: String? = null,
    @SerialName("trail_price") val trailPrice: String? = null,
    val hwm: String? = null,
    val subtag: String? = null,
    val source: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("position_intent") val positionIntent: String,
) : ApiResponse()

@Serializable
data class TakeProfit(
    @SerialName("limit_price") val limitPrice: String
) : ApiResponse()

@Serializable
data class StopLoss(
    @SerialName("stop_price") val stopPrice: String
) : ApiResponse()

@Serializable
data class OrderLeg(
    val side: String? = null,
    @SerialName("position_intent") val positionIntent: String? = null,
    val symbol: String? = null,
    @SerialName("radio_qty") val ratioQty: String? = null,
) : ApiResponse()

val types = listOf(
    "market", "limit", "stop", "stop_limit", "trailing_stop"
)

val sides = listOf(
    "buy", "sell"
)

val timeInForces = listOf(
    "day", "gtc", "opg", "cls", "ioc", "fok"
)

val orderClass = listOf(
    "simple", "bracket", "oco", "oto", "mleg"
)
