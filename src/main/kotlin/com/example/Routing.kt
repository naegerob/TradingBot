package com.example

import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.tradingLogic.TradingLogic
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(trader: TradingLogic) {

    routing {
        get("/AccountDetails") {
            val accountResponse = trader.fetchAccountDetails()
            respondToClient(accountResponse, call)
        }
        // TODO: Remove local variables of indicators and insert it directyl
        route("/Indicators") {
            get("Original") {
                val original = trader.mOriginalPrices
                call.respondText(original.toString(), status = HttpStatusCode.OK)
            }
            route("/Sma") {
                get("/Short") {
                    val smaShort = trader.mShortSMA
                    call.respondText(smaShort.toString(), status = HttpStatusCode.OK)
                }
                get("/Long") {
                    val smaLong = trader.mLongSMA
                    call.respondText(smaLong.toString(), status = HttpStatusCode.OK)
                }
            }
            route("/BollingerBands") {
                get("/Middle") {
                    val sma = trader.mAverageBollingerBand
                    call.respondText(sma.toString(), status = HttpStatusCode.OK)
                }
                get("/Upper") {
                    val upperBollinger = trader.mUpperBollingerBand
                    call.respondText(upperBollinger.toString(), status = HttpStatusCode.OK)
                }
                get("/Lower") {
                    val lowerBollinger = trader.mLowerBollingerBand
                    call.respondText(lowerBollinger.toString(), status = HttpStatusCode.OK)
                }
            }
            get("/Rsi") {
                val rsi = trader.mRsi
                call.respondText(rsi.toString(), status = HttpStatusCode.OK)
            }
        }

        route("/Order") {
            post("/SetAllParameter") {
                val orderRequest = call.receive<OrderRequest>()
                val isSuccessfulSet = trader.setOrderParameter(orderRequest)
                if(isSuccessfulSet) {
                    call.respond(orderRequest)
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)
            }
            get("/Create") {
                val orderResponse = trader.createOrder()
                respondToClient(orderResponse, call)
            }
        }
        route("/HistoricalBars") {
            post("/SetAllParameter") {
                val stockRequest = call.receive<StockAggregationRequest>()
                val isSuccessfulSet = trader.setAggregationParameter(stockRequest)
                if(isSuccessfulSet) {
                    call.respond(stockRequest)
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)
            }

            get("/Request") {
                val historicalBarsResponse = trader.getHistoricalBars()
                respondToClient(historicalBarsResponse, call)
            }
        }
    }
}

suspend fun respondToClient(httpResponse: HttpResponse, call: RoutingCall) {

    when (httpResponse.status) {
        HttpStatusCode.OK                   -> call.respond(HttpStatusCode.OK, httpResponse.bodyAsText())
        HttpStatusCode.BadRequest           -> call.respond(HttpStatusCode.BadRequest, "Parameter have wrong format. Check Alpaca Doc!")
        HttpStatusCode.MovedPermanently     -> call.respond(HttpStatusCode.MovedPermanently)
        HttpStatusCode.NotFound             -> call.respond(HttpStatusCode.NotFound)
        HttpStatusCode.Forbidden            -> call.respond(HttpStatusCode.Forbidden, "Buying power or shares is not sufficient.")
        HttpStatusCode.UnprocessableEntity  -> call.respond(HttpStatusCode.UnprocessableEntity, "Input parameters are not recognized.")
        else                                -> call.respond(HttpStatusCode.InternalServerError, "Error is not handled.")
    }
}