package com.example.data.singleModels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MarketHours(
    @SerialName("is_open") val isOpen: Boolean,
    @SerialName("next_close") val nextClose: String,
    @SerialName("next_open") val nextOpen: String,
    @SerialName("timestamp") val timestamp: String
)
