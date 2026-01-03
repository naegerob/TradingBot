package com.example.services

import com.example.data.alpaca.AlpacaRepository
import com.example.data.database.DataBaseFacade
import com.example.data.singleModels.*
import com.example.tradingLogic.TradingLogicError
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.example.tradingLogic.Result
import io.ktor.client.call.*

class TraderService : KoinComponent {

    private val mRepository by inject<AlpacaRepository>()
    private val mTransactionStorage by inject<DataBaseFacade>()

    suspend fun getAccountDetails(): Result<Account, TradingLogicError> {
        val httpResponse = mRepository.getAccountDetails()
        return when (httpResponse.status) {
            HttpStatusCode.OK -> Result.Success(httpResponse.body<Account>())
            else -> Result.Error(TradingLogicError.DataError.NO_SUFFICIENT_ACCOUNT_BALANCE)
        }
    }

    suspend fun createOrder(orderRequest: OrderRequest): Result<Unit, TradingLogicError> {
        val isOrderCommandRight = sides.contains(orderRequest.side)
        if (!isOrderCommandRight) {
            return Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
        }

        val quantity: String = orderRequest.quantity?.trim().orEmpty()
        val notional: String = orderRequest.notional?.trim().orEmpty()

        mTransactionStorage.addTransaction(
            symbol = orderRequest.symbol,
            side = orderRequest.side,
            quantity = quantity,
            notional = notional
        )
        val httpResponse = mRepository.createOrder(orderRequest)
        return when (httpResponse.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> Result.Success(Unit)
            HttpStatusCode.Forbidden -> Result.Error(TradingLogicError.DataError.NO_SUFFICIENT_ACCOUNT_BALANCE)
            HttpStatusCode.UnprocessableEntity, HttpStatusCode.BadRequest ->
                Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
            else -> Result.Error(TradingLogicError.DataError.MISC_ERROR)
        }
    }

    suspend fun getHistoricalData(historicalRequest: StockAggregationRequest, stock: String): Result<List<StockBar>, TradingLogicError> {
        val httpResponse = mRepository.getHistoricalData(historicalRequest)
        return when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val stockResponse = httpResponse.body<StockAggregationResponse>()
                if (stockResponse.bars[stock] == null) {
                    return Result.Error(TradingLogicError.DataError.NO_HISTORICAL_DATA_AVAILABLE)
                }
                Result.Success(stockResponse.bars[stock]!!)
            }
            HttpStatusCode.TooManyRequests -> Result.Error(TradingLogicError.DataError.HISTORICAL_DATA_TOO_MANY_REQUESTS)
            HttpStatusCode.BadRequest -> Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
            HttpStatusCode.Unauthorized -> Result.Error(TradingLogicError.DataError.INVALID_PARAMETER_FORMAT)
            else -> Result.Error(TradingLogicError.DataError.MISC_ERROR)
        }
    }
}