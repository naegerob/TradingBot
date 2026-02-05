package com.example.tradinglogic

typealias RootError = Error

// out let me declare more abstract types than required here
sealed interface Result<out D, out E : RootError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

