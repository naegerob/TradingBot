package com.example.tradingLogic

import com.example.data.AlpacaRepository
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.singleModels.StockAggregationResponse
import com.example.data.singleModels.StockBar
import com.example.tradingLogic.strategies.TradingStrategy
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


class TradingBot(
    private val mStockAggregationRequest: StockAggregationRequest,
    private val mStrategy: TradingStrategy,
    private val mIndicators: Indicators,
    private val mAlpacaRepository: AlpacaRepository
) {
    var mIsRunning = false

    private suspend fun getValidatedHistoricalBars(): List<StockBar> {
        val httpResponse = mAlpacaRepository.getHistoricalData(mStockAggregationRequest)
        try {
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val stockResponse = httpResponse.body<StockAggregationResponse>()
                    return stockResponse.bars[mIndicators.mStock]!!
                }
                else -> return emptyList() // TODO: is just bad
            }
        } catch (e: Exception) {
            println(e)
            return emptyList()
        }
    }
    fun run() {
        runBlocking {
            val historicalBars = getValidatedHistoricalBars()
            mIndicators.updateIndicators(historicalBars)
            val signal = mStrategy.executeAlgorithm(mIndicators)
            // TODO: Buy and hold
        }
    }
}

