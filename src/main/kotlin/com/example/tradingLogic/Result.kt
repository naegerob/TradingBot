package com.example.tradingLogic

typealias RootError = Error

// out let me declare more abstract types than required here
sealed interface Result<out D, out E: RootError> {
    data class Success<out D, out E: RootError>(val data: D): Result<D, E>
    data class Error<out D, out E: RootError>(val error: E): Result<D, E>
}

