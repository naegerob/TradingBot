package com.example.data

import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import io.ktor.client.HttpClient
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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// TODO: Consider passing client and Dispatcher for DI
class AlpacaRepository(private val mClient: HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
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
        header("content-type", "application/json")
        header("accept", "application/json")
    }
}) {

    private val paperBaseUrl = createPaperBaseUrl()
    private val paperBaseMarketUrl = createPaperMarketBaseUrl()

    companion object {
        private const val SCHEME = "https"
        private const val PAPERHOST = "paper-api.alpaca.markets"
        private const val PAPERMARKETHOST = "data.alpaca.markets"
        private const val BASEURLAPPENDIX = "v2"
        val PAPERAPIKEY = System.getenv("PAPERAPIKEY")
        val PAPERSECRET = System.getenv("PAPERSECRET")
        private val APIKEY = System.getenv("APIKEY")
        private val SECRET = System.getenv("SECRET")
    }

    private fun createPaperBaseUrl(): Url {
        return URLBuilder().apply {
            protocol = URLProtocol.createOrDefault(SCHEME)
            host = PAPERHOST
            encodedPath = BASEURLAPPENDIX
        }.build()
    }

    private fun createPaperMarketBaseUrl(): Url =
        URLBuilder().apply {
            protocol = URLProtocol.createOrDefault(SCHEME)
            host = PAPERMARKETHOST
            encodedPath = BASEURLAPPENDIX
        }.build()


    suspend fun getAccountDetails(): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.get(paperBaseUrl) {
                url {
                    appendPathSegments("account")
                }
            }
        }

    suspend fun getOpenPositions(): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.get(paperBaseUrl) {
                url {
                    appendPathSegments("positions")
                }
            }
        }

    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.post(paperBaseUrl) {
                url {
                    appendPathSegments("orders")
                }
                setBody(orderRequest)
            }
        }

    suspend fun getHistoricalData(historicalRequest: StockAggregationRequest)
            : HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.get(paperBaseMarketUrl) {
                url {
                    appendPathSegments("stocks", "bars")
                    parameters.append("symbols", historicalRequest.symbols)
                    parameters.append("timeframe", historicalRequest.timeframe)
                    historicalRequest.startDateTime?.let { parameters.append("start", it) }
                    historicalRequest.endDateTime?.let { parameters.append("end", it) }
                    parameters.append("limit", historicalRequest.limit.toString())
                    parameters.append("adjustment", historicalRequest.adjustment)
                    historicalRequest.asOfDate?.let { parameters.append("asof", it) }
                    parameters.append("feed", historicalRequest.feed)
                    parameters.append("currency", historicalRequest.currency)
                    historicalRequest.pageToken?.let { parameters.append("page_token", it) }
                    parameters.append("sort", StockAggregationRequest().sort) // Hardcode to newest datapoint at end of list
                }
            }
        }
}