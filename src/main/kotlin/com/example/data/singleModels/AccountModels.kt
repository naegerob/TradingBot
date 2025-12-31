package com.example.data.singleModels

import com.example.data.alpaca.ApiResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
) : ApiResponse()


@Serializable
data class OpeningHours(
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("next_open") val nextOpen: String = "",
    @SerialName("next_close") val nextClose: String = "",
    @SerialName("timestamp") val timestamp: String = ""
) : ApiResponse()