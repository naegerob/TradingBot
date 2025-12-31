package com.example.data.alpaca

import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlpacaRepository : KoinComponent {

    private val mClient by inject<HttpClient>()
    private val paperBaseUrl = createPaperBaseUrl()
    private val paperBaseMarketUrl = createPaperMarketBaseUrl()

    companion object {
        private const val SCHEME = "https"
        private const val PAPERHOST = "paper-api.alpaca.markets"
        private const val PAPERMARKETHOST = "data.alpaca.markets"
        private const val BASEURLAPPENDIX = "v2"
        val PAPERAPIKEY = System.getenv("PAPERAPIKEY") ?: System.getenv("paperapikey")
        val PAPERSECRET = System.getenv("PAPERSECRET") ?: System.getenv("papersecret")
        private val APIKEY = System.getenv("APIKEY") ?: System.getenv("apikey")
        private val SECRET = System.getenv("SECRET") ?: System.getenv("secret")
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
                    parameters.append(
                        "sort",
                        StockAggregationRequest().sort
                    ) // Hardcode to newest datapoint at end of list
                }
            }
        }

    suspend fun getMarketOpeningHours(): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.get(paperBaseUrl) {
                url {
                    appendPathSegments("clock")
                }
            }
        }
}