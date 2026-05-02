package com.example.tradinglogic

import com.example.data.alpaca.OrderRequest
import com.example.data.alpaca.OrderResponse
import com.example.data.alpaca.StockAggregationRequest
import com.example.services.TraderService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TradingController : KoinComponent {

    private val mTraderService by inject<TraderService>()
    val mTradingBot by inject<TradingBot>()

    suspend fun createOrder(orderRequest: OrderRequest): Result<OrderResponse, TradingLogicError> =
        mTraderService.createOrder(orderRequest)

    suspend fun getStockData(stockAggregationRequest: StockAggregationRequest) =
        mTraderService.getHistoricalData(stockAggregationRequest)

    suspend fun fetchAccountDetails() =
        mTraderService.getAccountDetails()

    suspend fun getMarketOpeningHours() = mTraderService.getMarketOpeningHours()

    suspend fun doBacktesting(
        backtestConfig: BacktestConfig
    ): Result<Any, TradingLogicError> {
        return mTradingBot.backtest(backtestConfig)
    }

    fun isBotRunning(): Boolean = mTradingBot.isRunning()

    suspend fun startBot() = mTradingBot.run()

    suspend fun stopBot() = mTradingBot.stop()

    fun setBotConfig(botConfig: BotConfig): Boolean = mTradingBot.updateConfig(botConfig)

    fun isBotConfigured(): Boolean = mTradingBot.isConfigured()
}
