package com.example.data.database

import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.Instant

class DataBaseImpl : DataBaseFacade {

    private fun toTransaction(row: ResultRow): Transaction =
        Transaction(
            id = row[TransactionsTable.id],
            createdAt = Instant.ofEpochMilli(row[TransactionsTable.createdAt]),
            symbol = row[TransactionsTable.symbol],
            side = row[TransactionsTable.side],
            quantity = row[TransactionsTable.quantity].toPlainString(),
            notional = row[TransactionsTable.notional].toPlainString()
        )

    private fun toToken(row: ResultRow): Token =
        Token(
            tokenId = row[TokenTable.tokenId],
            createdAt = Instant.ofEpochMilli(row[TokenTable.createdAt]),
            token = row[TokenTable.token].toString()
        )

    override suspend fun allTransactions(): List<Transaction> = DatabaseFactory.dbQuery {
        TransactionsTable
            .selectAll()
            .orderBy(TransactionsTable.id)
            .map(::toTransaction)
    }

    override suspend fun getTransaction(id: Int): Transaction? = DatabaseFactory.dbQuery {
        TransactionsTable
            .selectAll()
            .where { TransactionsTable.id eq id }
            .map(::toTransaction)
            .singleOrNull()
    }

    override suspend fun addTransaction(symbol: String, side: String, quantity: String, notional: String): Transaction = DatabaseFactory.dbQuery {
        val now = Instant.now()
        val newId = TransactionsTable.insert {
            it[createdAt] = now.toEpochMilli()
            it[TransactionsTable.symbol] = symbol
            it[TransactionsTable.side] = side
            it[TransactionsTable.quantity] = quantity.toBigDecimalOrNullIfBlank()
            it[TransactionsTable.notional] = notional.toBigDecimalOrNullIfBlank()
        }[TransactionsTable.id]
        Transaction(
            id = newId,
            createdAt = now,
            symbol = symbol,
            side = side,
            quantity = quantity,
            notional = notional
        )
    }

    override suspend fun deleteTransaction(id: Int): Boolean = DatabaseFactory.dbQuery {
        TransactionsTable.deleteWhere { TransactionsTable.id eq id } > 0
    }

    override suspend fun deleteAllTransactions(): Boolean = DatabaseFactory.dbQuery {
        TransactionsTable.deleteAll()
        true
    }

    override suspend fun doesTransactionExist(id: Int): Boolean = DatabaseFactory.dbQuery {
        TransactionsTable
            .selectAll()
            .where { TransactionsTable.id eq id }
            .empty().not()
    }

    override suspend fun getToken(tokenId: String): Token? = DatabaseFactory.dbQuery {
        TokenTable.selectAll()
            .where { TokenTable.tokenId eq tokenId }
            .map(::toToken)
            .singleOrNull()
    }

    override suspend fun addToken(tokenId: String, token: String): Token = DatabaseFactory.dbQuery {
        val now = Instant.now()
        TokenTable.insert {
            it[createdAt] = now.toEpochMilli()
            it[TokenTable.tokenId] = tokenId
            it[TokenTable.token] = token
        }[TokenTable.tokenId]
        Token(
            tokenId = tokenId,
            createdAt = now,
            token = token.toString()
        )
    }

    override suspend fun deleteToken(tokenId: String): Boolean = DatabaseFactory.dbQuery {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllTokens(): Boolean = DatabaseFactory.dbQuery {
        TODO("Not yet implemented")
    }

    override suspend fun doesTokenExist(tokenId: String): Boolean = DatabaseFactory.dbQuery {
        TODO("Not yet implemented")
    }

    private fun String.toBigDecimalOrNullIfBlank(): BigDecimal {
        val s = trim()
        if (s.isEmpty()) return "0".toBigDecimal()
        return s.replace(',', '.').toBigDecimal()
    }

}
