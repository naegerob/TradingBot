package com.example.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// TODO: separate to different files
@Serializable
open class ApiResponse

@Serializable
data class Account(
    @SerialName("id") val id: String,
    @SerialName("admin_configurations") val adminConfigurations: Map<String, String> = emptyMap(),
    @SerialName("user_configurations") val userConfigurations: String? = null,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("status") val status: String,
    @SerialName("crypto_status") val cryptoStatus: String,
    @SerialName("options_approved_level") val optionsApprovedLevel: Int,
    @SerialName("options_trading_level") val optionsTradingLevel: Int,
    @SerialName("currency") val currency: String,
    @SerialName("buying_power") val buyingPower: String,
    @SerialName("regt_buying_power") val regtBuyingPower: String,
    @SerialName("daytrading_buying_power") val dayTradingBuyingPower: String,
    @SerialName("effective_buying_power") val effectiveBuyingPower: String,
    @SerialName("non_marginable_buying_power") val nonMarginableBuyingPower: String,
    @SerialName("options_buying_power") val optionsBuyingPower: String,
    @SerialName("bod_dtbp") val bodDtbp: String,
    @SerialName("cash") val cash: String,
    @SerialName("accrued_fees") val accruedFees: String,
    @SerialName("portfolio_value") val portfolioValue: String,
    @SerialName("pattern_day_trader") val patternDayTrader: Boolean,
    @SerialName("trading_blocked") val tradingBlocked: Boolean,
    @SerialName("transfers_blocked") val transfersBlocked: Boolean,
    @SerialName("account_blocked") val accountBlocked: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("trade_suspended_by_user") val tradeSuspendedByUser: Boolean,
    @SerialName("multiplier") val multiplier: String,
    @SerialName("shorting_enabled") val shortingEnabled: Boolean,
    @SerialName("equity") val equity: String,
    @SerialName("last_equity") val lastEquity: String = "",  // Add this default to match partial data
    @SerialName("long_market_value") val longMarketValue: String = "",
    @SerialName("short_market_value") val shortMarketValue: String = "",
    @SerialName("position_market_value") val positionMarketValue: String = "",
    @SerialName("initial_margin") val initialMargin: String = "",
    @SerialName("maintenance_margin") val maintenanceMargin: String = "",
    @SerialName("last_maintenance_margin") val lastMaintenanceMargin: String = "",
    @SerialName("sma") val sma: String = "",
    @SerialName("daytrade_count") val dayTradeCount: Int = 0,
    @SerialName("balance_asof") val balanceAsOf: String = "",
    @SerialName("crypto_tier") val cryptoTier: Int = 0,
    @SerialName("intraday_adjustments") val intradayAdjustments: String,
    @SerialName("pending_reg_taf_fees") val pendingRegTafFees: String
)

@Serializable
data class AssetPosition(
    @SerialName("asset_id") val assetId: String,
    val symbol: String,
    val exchange: String,
    @SerialName("asset_class") val assetClass: String,
    @SerialName("avg_entry_price") val avgEntryPrice: String,
    val qty: String,
    @SerialName("qty_available") val qtyAvailable: String? = null,
    val side: String,
    @SerialName("market_value") val marketValue: String,
    @SerialName("cost_basis") val costBasis: String,
    @SerialName("unrealized_pl") val unrealizedPL: String,
    @SerialName("unrealized_plpc") val unrealizedPLPC: String,
    @SerialName("unrealized_intraday_pl") val unrealizedIntradayPL: String,
    @SerialName("unrealized_intraday_plpc") val unrealizedIntradayPLPC: String,
    @SerialName("current_price") val currentPrice: String,
    @SerialName("lastday_price") val lastdayPrice: String,
    @SerialName("change_today") val changeToday: String,
    @SerialName("asset_marginable") val assetMarginable: Boolean
)

@Serializable
data class OrderRequest(
    @SerialName("side") var side: String = "buy",
    @SerialName("type") var type: String = "market",
    @SerialName("time_in_force") var timeInForce: String = "day",
    @SerialName("qty") var quantity: String = "1",
    @SerialName("symbol") var symbol: String = "AAPL",
    @SerialName("limit_price") val limitPrice: String? = null,
    @SerialName("stop_price") val stopPrice: String? = null,
    @SerialName("trail_price") val trailPrice: String? = null,
    @SerialName("trail_percent") val trailPercent: String? = null,
    @SerialName("extended_hours") val extendedHours: Boolean = false,
    @SerialName("client_order_id") val clientOrderId: String? = null,
    @SerialName("order_class") val orderClass: String? = null,
    @SerialName("take_profit") val takeProfit: TakeProfit? = null,
    @SerialName("stop_loss") val stopLoss: StopLoss? = null,
    @SerialName("position_intent") val positionIntent: String? = null
)

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
    val qty: String,
    @SerialName("filled_qty") val filledQty: String,
    @SerialName("filled_avg_price") val filledAvgPrice: String? = null,
    @SerialName("order_class") val orderClass: String,
    @SerialName("order_type") val orderType: String,
    val type: String,
    val side: String,
    @SerialName("position_intent") val positionIntent: String,
    @SerialName("time_in_force") val timeInForce: String,
    @SerialName("limit_price") val limitPrice: String? = null,
    @SerialName("stop_price") val stopPrice: String? = null,
    val status: String,
    @SerialName("extended_hours") val extendedHours: Boolean,
    val legs: String? = null,
    @SerialName("trail_percent") val trailPercent: String? = null,
    @SerialName("trail_price") val trailPrice: String? = null,
    val hwm: String? = null,
    val subtag: String? = null,
    val source: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
): ApiResponse()

@Serializable
data class InsufficientBuyingPowerResponse(
    @SerialName("buying_power") val buyingPower: String,
    @SerialName("code") val code: Int,
    @SerialName("cost_basis") val costBasis: String,
    @SerialName("message") val message: String
): ApiResponse()

@Serializable
data class TakeProfit(
    @SerialName("limit_price") val limitPrice: String
)

@Serializable
data class StopLoss(
    @SerialName("stop_price") val stopPrice: String
)

@Serializable
data class StockAggregationRequest(
    @SerialName("symbols")
    val symbols: String = "AAPL", // Comma-separated list of stock symbols (e.g., "TSLA,AMZN")
    @SerialName("timeframe")
    val timeframe: String = "5Min", // Timeframe for aggregation (e.g., "5Min", "1D", "3M")
    @SerialName("start")
    val startDateTime: String? = null, // Inclusive start date-time (RFC-3339 or "YYYY-MM-DD")
    @SerialName("end")
    val endDateTime: String? = null, // Inclusive end date-time (RFC-3339 or "YYYY-MM-DD")
    @SerialName("limit")
    val limit: Int = 1000, // Maximum number of data points to return (default: 1000)
    @SerialName("adjustment")
    val adjustment: String = "raw", // Corporate action adjustment (default: "raw")
    @SerialName("asof")
    val asOfDate: String? = null, // As-of date to identify the underlying entity (format: "YYYY-MM-DD")
    @SerialName("feed")
    val feed: String = "sip", // Data feed source (default: "sip")
    @SerialName("currency")
    val currency: String = "USD", // Currency of prices (default: "USD")
    @SerialName("page_token")
    val pageToken: String? = null, // Pagination token for continuing a request
    @SerialName("sort")
    val sort: String = "asc" // Sort order (default: "asc")
): ApiResponse()

@Serializable
data class StockAggregationResponse(
    val bars: Map<String, List<StockBar>>, // Dynamic key for each stock symbol
    @SerialName("next_page_token") val nextPageToken: String? = null
): ApiResponse()

@Serializable
data class StockBar(
    @SerialName("c") val close: Double,      // Closing price
    @SerialName("h") val high: Double,       // Highest price
    @SerialName("l") val low: Double,        // Lowest price
    @SerialName("n") val trades: Int,        // Number of trades
    @SerialName("o") val open: Double,       // Opening price
    @SerialName("t") val timestamp: String,  // Timestamp (ISO 8601)
    @SerialName("v") val volume: Int,        // Volume
    @SerialName("vw") val vwap: Double       // Volume-weighted average price
): ApiResponse()

@Serializable
data class ErrorResponse(
    val message: String
)

val types = listOf(
    "market", "limit", "stop", "stop_limit", "trailing_stop"
)

val sides = listOf(
    "buy", "sell"
)

val timeInForces = listOf(
    "day", "gtc", "opg", "cls", "ioc", "fok"
)

val extended_hours = listOf(
    true, false
)
val sorts = listOf(
    "", "asc", "desc"
)

val timeframes = listOf(
    "Min", "T", "Hours", "H", "Day", "D", "Week", "W", "Month", "M",
)

val feeds = listOf(
    "", "iex", "otc", "sip"
)


