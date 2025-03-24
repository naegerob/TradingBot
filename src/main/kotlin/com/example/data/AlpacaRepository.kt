package com.example.data

import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlpacaRepository(private val mClient: HttpClient) : TradingRepository {

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


    override suspend fun getAccountDetails(): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.get(paperBaseUrl) {
                url {
                    appendPathSegments("account")
                }
            }
        }

    override suspend fun getOpenPositions(): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.get(paperBaseUrl) {
                url {
                    appendPathSegments("positions")
                }
            }
        }

    override suspend fun createOrder(orderRequest: OrderRequest): HttpResponse =
        withContext(Dispatchers.IO) {
            mClient.post(paperBaseUrl) {
                url {
                    appendPathSegments("orders")
                }
                setBody(orderRequest)
            }
        }

    override suspend fun getHistoricalData(historicalRequest: StockAggregationRequest)
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