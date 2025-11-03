package com.example.data

import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import io.ktor.client.statement.*

interface TradingRepository {

    suspend fun getAccountDetails(): HttpResponse

    suspend fun getOpenPositions(): HttpResponse

    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse

    suspend fun getHistoricalData(historicalRequest: StockAggregationRequest): HttpResponse

    suspend fun getMarketOpeningHours(): HttpResponse
}