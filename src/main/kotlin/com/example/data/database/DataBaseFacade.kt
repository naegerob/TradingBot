package com.example.data.database

interface DataBaseFacade {
    suspend fun allTransactions(): List<Transaction>

    suspend fun transaction(id: Int): Transaction?

    suspend fun addTransaction(
        symbol: String,
        side: String,
        quantity: String?
    ): Transaction

    suspend fun deleteTransaction(id: Int): Boolean

    suspend fun deleteAllTransactions(): Boolean

    suspend fun doesTransactionExist(id: Int): Boolean
}