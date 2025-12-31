package com.example.data.database


class DataBaseImpl : DataBaseFacade {
    override suspend fun allTransactions(): List<Transaction> {
        TODO("Not yet implemented")
    }

    override suspend fun transaction(id: Int): Transaction? {
        TODO("Not yet implemented")
    }

    override suspend fun addTransaction(symbol: String, side: String, quantity: String?): Transaction {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTransaction(id: Int): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllTransactions(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun doesTransactionExist(id: Int): Boolean {
        TODO("Not yet implemented")
    }
}

