package com.example.data.database

import java.time.Instant
import org.jetbrains.exposed.sql.Table

object TokenTable : Table("tokens") {
    val tokenId = varchar("token_id",
        length = 255
    )
    val createdAt = long("created_at") // epoch millis
    val token = varchar(
        "token",
        length = 255
    )

    override val primaryKey = PrimaryKey(tokenId)
}

data class Token(
    val tokenId: String,
    val createdAt: Instant,
    val token: String
)