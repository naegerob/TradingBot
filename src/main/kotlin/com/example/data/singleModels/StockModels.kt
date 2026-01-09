package com.example.data.singleModels

import com.example.data.alpaca.ApiResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockAggregationRequest(
    @SerialName("symbols") var symbols: String = "AAPL", // Comma-separated list of stock symbols (e.g., "TSLA,AMZN")
    @SerialName("timeframe") val timeframe: String = "5Min", // Timeframe for aggregation (e.g., "5Min", "1D", "3M")
    @SerialName("start") val startDateTime: String? = "2024-01-03T00:00:00Z", // Inclusive start date-time (RFC-3339 or "YYYY-MM-DD")
    @SerialName("end") val endDateTime: String? = "2024-01-04T00:00:00Z", // Inclusive end date-time (RFC-3339 or "YYYY-MM-DD")
    @SerialName("limit") val limit: Int = 1000, // Maximum number of data points to return (default: 1000)
    @SerialName("adjustment") val adjustment: String = "raw", // Corporate action adjustment (default: "raw")
    @SerialName("asof") val asOfDate: String? = null, // As-of date to identify the underlying entity (format: "YYYY-MM-DD")
    @SerialName("feed") val feed: String = "sip", // Data feed source (default: "sip")
    @SerialName("currency") val currency: String = "USD", // Currency of prices (default: "USD")
    @SerialName("page_token") val pageToken: String? = null, // Pagination token for continuing a request
    @SerialName("sort") val sort: String = "asc" // Sort order: Newest at end
) : ApiResponse()

@Serializable
data class StockAggregationResponse(
    val bars: Map<String, List<StockBar>>, // Dynamic key for each stock symbol
    @SerialName("next_page_token") val nextPageToken: String? = null
) : ApiResponse()

@Serializable
data class StockBar(
    @SerialName("c") val close: Double,      // Closing price
    @SerialName("h") val high: Double,       // Highest price
    @SerialName("l") val low: Double,        // Lowest price
    @SerialName("n") val trades: Int,        // Number of trades
    @SerialName("o") val open: Double,       // Opening price
    @SerialName("t") val timestamp: String,  // Timestamp (ISO 8601)
    @SerialName("v") val volume: Int,        // Volume
    @SerialName("vw") val vwap: Double       // Volume-weighted average price
) : ApiResponse()


val sorts = listOf(
    "asc", "desc"  // Remove empty string since it causes validation issues
)

val timeframes = listOf(
    "Min", "T", "Hours", "H", "Day", "D", "Week", "W", "Month", "M",
)

val feeds = listOf(
    "", "iex", "otc", "sip"
)
