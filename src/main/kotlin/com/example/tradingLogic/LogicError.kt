package com.example.tradingLogic

sealed interface Error

sealed interface TradingLogicError : Error {
    enum class DataError : TradingLogicError {
        NO_SUFFICIENT_ACCOUNT_BALANCE,
        NO_HISTORICAL_DATA_AVAILABLE,
        INVALID_PARAMETER_FORMAT,
        HISTORICAL_DATA_TOO_MANY_REQUESTS,
        UNAUTHORIZED,
        MISC_ERROR
    }

    enum class StrategyError : TradingLogicError {
        NO_STRATEGY_SELECTED,
        WRONG_SYMBOLS_PROVIDED,
        NO_SYMBOLS_PROVIDED
    }

    enum class RunError : TradingLogicError {
        ALREADY_RUNNING,
        TIME_FRAME_COULD_NOT_PARSED,

    }
}