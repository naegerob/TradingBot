package com.example.tradinglogic

import com.example.data.alpaca.AlpacaRepository
import com.example.data.singleModels.*
import io.ktor.client.statement.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TradingController : KoinComponent {

    private val mAlpacaRepo by inject<AlpacaRepository>()

    val mTradingBot by inject<TradingBot>()

    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse {
        return mAlpacaRepo.createOrder(orderRequest)
    }

    suspend fun getStockData(stockAggregationRequest: StockAggregationRequest): HttpResponse {
        return mAlpacaRepo.getHistoricalData(stockAggregationRequest)
    }

    suspend fun fetchAccountDetails(): HttpResponse {
        return mAlpacaRepo.getAccountDetails()
    }

    suspend fun getOpeningHours(): HttpResponse {
        return mAlpacaRepo.getMarketOpeningHours()
    }

    suspend fun doBacktesting(
        backtestConfig: BacktestConfig
    ): Result<Any, TradingLogicError> {
        return mTradingBot.backtest(backtestConfig)
    }

    fun startBot() {
        mTradingBot.run()
    }

    fun stopBot() {
        mTradingBot.stop()
    }
}
