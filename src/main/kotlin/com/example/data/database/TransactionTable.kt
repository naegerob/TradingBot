package com.example.data.database

import org.jetbrains.exposed.sql.Table
import java.time.Instant

object TransactionsTable : Table("transactions") {
    val id = integer("id").autoIncrement()
    val createdAt = long("created_at") // epoch millis
    val symbol = varchar("symbol", length = 32)
    val side = varchar("side", length = 8) // BUY / SELL
    val quantity = decimal("quantity", precision = 18, scale = 8)
    val price = decimal("price", precision = 18, scale = 8)

    override val primaryKey = PrimaryKey(id)
}

data class Transaction(
    val id: Int,
    val createdAt: Instant,
    val symbol: String,
    val side: String,
    val quantity: String,
    val price: String
)