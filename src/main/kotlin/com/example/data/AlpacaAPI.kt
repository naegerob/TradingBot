package org.example.finance.datamodel

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.encodedPath
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AlpacaAPI {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        engine {
            requestTimeout = 0 // 0 to disable, or a millisecond value to fit your needs
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        install(DefaultRequest) {
            header("APCA-API-KEY-ID", PAPERAPIKEY)
            header("APCA-API-SECRET-KEY", PAPERSECRET)

        }
    }
    private val paperBaseUrl = createPaperBaseUrl()
    private val paperBaseMarketUrl = createPaperMarketBaseUrl()

    companion object {
        private const val SCHEME = "https"
        private const val PAPERHOST = "paper-api.alpaca.markets"
        private const val PAPERMARKETHOST = "data.alpaca.markets"
        private const val BASEURLAPPENDIX = "v2"
        private const val PAPERAPIKEY = "PK44IDD5ECKOR1IDIZ6Z"
        private const val PAPERSECRET = "DILoINnEdZBtFacrp1zAD7HiXF2UH5bmEed6CAOy"
        private const val APIKEY = "AKFKVCZGHSW6OHQQS4K0"
        private const val SECRET = "Pu5t7eaRNqxdcmLgjwlm8ohsdW5Kz0leaSfw14EU"
    }

    private fun createPaperBaseUrl(): Url {
        return URLBuilder().apply {
            protocol = URLProtocol.createOrDefault(SCHEME)
            host = PAPERHOST
            encodedPath = BASEURLAPPENDIX
        }.build()
    }

    private fun createPaperMarketBaseUrl(): Url {
        return URLBuilder().apply {
            protocol = URLProtocol.createOrDefault(SCHEME)
            host = PAPERMARKETHOST
            encodedPath = BASEURLAPPENDIX
        }.build()
    }

    suspend fun getAccountDetails() : Account? {
        try {
            val httpResponse = client.get(paperBaseUrl) {
                url {
                    appendPathSegments("account")
                }
                headers {
                    append("accept", "application/json")
                }
            }
            return httpResponse.body<Account>()
        } catch (e: Exception) {
            println(e.message)
            return null
        }
    }

    suspend fun getOpenPositions(): AssetPosition? {
        try {
            val httpResponse = client.get(paperBaseUrl) {
                url {
                    appendPathSegments("positions")
                }
                headers {
                    append("accept", "application/json")
                }
            }
            return httpResponse.body<AssetPosition>()
        } catch (e: Exception) {
            println(e.message)
            return null
        }
    }

    suspend fun createOrder(
        symbol: String,
        quantity: String,
        side: String,
        timeInForce: String,
        orderType: String,
        limitOrStopValue: Double,
        stopLoss: Double
    ): OrderResponse? {

        val orderRequest = OrderRequest(
            side = side,
            orderType = orderType,
            timeInForce = timeInForce,
            quantity = quantity,
            symbol = symbol,
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            extendedHours = false,
            clientOrderId = null,
            orderClass = "",
            takeProfit = null,
            stopLoss = null,
            positionIntent = null
        )
        try {
            val httpResponse = client.post(paperBaseUrl) {
                url {
                    appendPathSegments("orders")
                }
                headers {
                    append("accept", "application/json")
                    append("content-type", "application/json")
                }
                val jsonOrderRequest = Json.encodeToString(orderRequest)
                setBody(jsonOrderRequest)
            }

            return httpResponse.body<OrderResponse>()
        } catch (e: Exception) {
            println(e.message)
            return null
        }
    }

    suspend fun getHistoricalData(
        symbols: String,
        timeframe: String,
        startDateTime: String?,
        endDateTime: String?
    ): StockAggregationResponse? {
        val stockDataPoint = StockAggregationRequest(
            symbols = symbols,
            timeframe = timeframe,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            limit = 1000, // Maximum number of data points to return (default: 1000)
            adjustment = "raw", // Corporate action adjustment (default: "raw")
            asOfDate = null, // As-of date to identify the underlying entity (format: "YYYY-MM-DD")
            feed = "sip", // Data feed source (default: "sip")
            currency = "USD", // Currency of prices (default: "USD")
            pageToken = null, // Pagination token for continuing a request
            sort = "asc" // Sort order (default: "asc")
        )
        try {
            val httpResponse = client.get(paperBaseMarketUrl) {
                url {
                    appendPathSegments("stocks", "bars")

                    parameters.append("symbols", stockDataPoint.symbols)
                    parameters.append("timeframe", stockDataPoint.timeframe)
                    stockDataPoint.startDateTime?.let { parameters.append("start", it) }
                    stockDataPoint.endDateTime?.let { parameters.append("end", it) }
                    parameters.append("limit", stockDataPoint.limit.toString())
                    parameters.append("adjustment", stockDataPoint.adjustment)
                    stockDataPoint.asOfDate?.let { parameters.append("asof", it) }
                    parameters.append("feed", stockDataPoint.feed)
                    parameters.append("currency", stockDataPoint.currency)
                    stockDataPoint.pageToken?.let { parameters.append("page_token", it) }
                    parameters.append("sort", stockDataPoint.sort)
                }
                headers {
                    append("accept", "application/json")
                }
            }

            return httpResponse.body<StockAggregationResponse>()
        } catch (e: IllegalArgumentException){
            println(e.message)
            return null
        } catch(e: Exception) {
            println(e.message)
            return null
        }
    }
}