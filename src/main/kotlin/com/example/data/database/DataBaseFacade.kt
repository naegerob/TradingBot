package com.example.data.database

interface DataBaseFacade {
    suspend fun allTransactions(): List<Transaction>

    suspend fun getTransaction(id: Int): Transaction?

    suspend fun addTransaction(
        symbol: String,
        side: String,
        quantity: String,
        notional: String
    ): Transaction

    suspend fun deleteTransaction(id: Int): Boolean

    suspend fun deleteAllTransactions(): Boolean

    suspend fun doesTransactionExist(id: Int): Boolean

    suspend fun getToken(tokenId: String): Token?

    suspend fun addToken(tokenId: String, token: String): Token

    suspend fun deleteToken(tokenId: String): Boolean

    suspend fun deleteAllTokens() : Boolean

    suspend fun doesTokenExist(tokenId: String) : Boolean
}