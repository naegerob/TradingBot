package com.example.tradingLogic

import com.example.data.AlpacaRepository
import com.example.data.singleModels.StockAggregationRequest
import com.example.tradingLogic.strategies.TradingStrategy
import kotlinx.coroutines.runBlocking


class TradingBot(
    private val mStrategy: TradingStrategy,
    private val mIndicators: Indicators,
    private val mAlpacaRepository: AlpacaRepository
) {
    var mIsRunning = false

    fun run() {
        runBlocking {
            val stockAggregationRequest = StockAggregationRequest()
            val historicalData = mAlpacaRepository.getHistoricalData(stockAggregationRequest)
            val signal = mStrategy.executeAlgorithm()
        }
    }
}

