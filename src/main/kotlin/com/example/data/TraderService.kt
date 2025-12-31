package com.example.data

import com.example.data.alpaca.AlpacaRepository
import com.example.data.database.DataBaseFacade
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import io.ktor.client.statement.HttpResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TraderService : KoinComponent {

    private val mRepository by inject<AlpacaRepository>()
    private val mTransactionStorage by inject<DataBaseFacade>()

    suspend fun getAccountDetails(): HttpResponse {
        return mRepository.getAccountDetails()
    }

    suspend fun getOpenPositions(): HttpResponse {
        return mRepository.getOpenPositions()
    }

    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse {
        val quantity: String = orderRequest.quantity?.trim().orEmpty()
        val notional: String = orderRequest.notional?.trim().orEmpty()

        mTransactionStorage.addTransaction(
            symbol = orderRequest.symbol,
            side = orderRequest.side,
            quantity = quantity,
            notional = notional
        )
        return mRepository.createOrder(orderRequest)
    }

    suspend fun getHistoricalData(historicalRequest: StockAggregationRequest): HttpResponse {
        return mRepository.getHistoricalData(historicalRequest)
    }

    suspend fun getMarketOpeningHours(): HttpResponse {
        return mRepository.getMarketOpeningHours()
    }
}