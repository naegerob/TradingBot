package com.example.data.database

import org.h2.expression.function.SubstringFunction
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object TransactionsTable : Table("transactions") {
    val id = integer("id").autoIncrement()
    val createdAt = long("created_at") // epoch millis
    val symbol = varchar("symbol", length = 32)
    val side = varchar("side", length = 8) // BUY / SELL
    val quantity = decimal("quantity", precision = 8, scale = 2)
    val notional = decimal("notional", precision = 8, scale = 2)

    override val primaryKey = PrimaryKey(id)
}

data class Transaction(
    val id: Int,
    val createdAt: Instant,
    val symbol: String,
    val side: String,
    val quantity: String,
    val notional: String
)