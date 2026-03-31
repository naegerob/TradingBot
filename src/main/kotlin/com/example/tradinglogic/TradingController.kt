package com.example.tradinglogic

import com.example.data.alpaca.AlpacaRepository
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.services.TraderService
import com.example.tradinglogic.Result
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TradingController : KoinComponent {

    private val mAlpacaRepo by inject<AlpacaRepository>()
    private val mTraderService by inject<TraderService>()
    val mTradingBot by inject<TradingBot>()

    suspend fun createOrder(orderRequest: OrderRequest):  {

        return when (val orderResponse = mTraderService.createOrder(orderRequest)) {
            is Result.Error -> HttpStatusCode(
                HttpStatusCode.BadRequest,
                description = "Invalid Parameter order request"
            )
            is Result.Success -> HttpStatusCode(
                HttpStatusCode.OK,
                description = TODO()
            )
        }
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

    fun isBotRunning(): Boolean {
        return mTradingBot.isRunning()
    }

    suspend fun startBot() = mTradingBot.run()


    suspend fun stopBot() {
        mTradingBot.stop()
    }

    fun setBotConfig(botConfig: BotConfig) : Boolean = mTradingBot.updateConfig(botConfig)

    fun isBotConfigured() : Boolean = mTradingBot.isConfigured()
}
