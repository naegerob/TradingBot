package com.example.tradingLogic

sealed interface Error

sealed interface TradingLogicError : Error {
    enum class DataError : TradingLogicError {
        NO_SUFFICIENT_ACCOUNT_BALANCE,
        NO_HISTORICAL_DATA_AVAILABLE,
        NO_HISTORICAL_DATA_EXCEPTION,
        MISC_ERROR
    }
}