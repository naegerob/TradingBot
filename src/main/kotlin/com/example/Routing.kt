package com.example

import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.tradingLogic.BacktestConfig
import com.example.tradingLogic.TradingController
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(tradingController: TradingController) {

    routing {
        get("/AccountDetails") {
            val accountResponse = tradingController.fetchAccountDetails()
            respondToClient(accountResponse, call)
        }
        // TODO: Remove local variables of indicators and insert it directyl
        route("/Indicators") {
            get("/Original") {
                val original = tradingController.mIndicators.mOriginalPrices
                call.respondText(original.toString(), status = HttpStatusCode.OK)
            }
            get("/Support") {
                val support = tradingController.mIndicators.mSupports
                call.respondText(support.toString(), status = HttpStatusCode.OK)
            }
            get("/Resistance") {
                val resistance = tradingController.mIndicators.mResistances
                call.respondText(resistance.toString(), status = HttpStatusCode.OK)
            }
            route("/Sma") {
                get("/Short") {
                    val smaShort = tradingController.mIndicators.mShortSMA
                    call.respondText(smaShort.toString(), status = HttpStatusCode.OK)
                }
                get("/Long") {
                    val smaLong = tradingController.mIndicators.mLongSMA
                    call.respondText(smaLong.toString(), status = HttpStatusCode.OK)
                }
            }
            route("/BollingerBands") {
                get("/Middle") {
                    val sma = tradingController.mIndicators.mAverageBollingerBand
                    call.respondText(sma.toString(), status = HttpStatusCode.OK)
                }
                get("/Upper") {
                    val upperBollinger = tradingController.mIndicators.mUpperBollingerBand
                    call.respondText(upperBollinger.toString(), status = HttpStatusCode.OK)
                }
                get("/Lower") {
                    val lowerBollinger = tradingController.mIndicators.mLowerBollingerBand
                    call.respondText(lowerBollinger.toString(), status = HttpStatusCode.OK)
                }
            }
            get("/Rsi") {
                val rsi = tradingController.mIndicators.mRsi
                call.respondText(rsi.toString(), status = HttpStatusCode.OK)
            }
        }

        route("/Order") {
            post("/Create") {
                val orderRequest = call.receive<OrderRequest>()
                val isValidRequest = tradingController.areValidOrderParameter(orderRequest)
                if(isValidRequest) {
                    val orderResponse = tradingController.createOrder(orderRequest)
                    respondToClient(orderResponse, call)
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)
            }
        }
        route("/HistoricalBars") {
            post("/Request") {
                val stockRequest = call.receive<StockAggregationRequest>()
                println(stockRequest)
                val isSuccessfulSet = tradingController.areValidStockRequestParameter(stockRequest)
                if(isSuccessfulSet) {
                    val stockResponse = tradingController.getStockData(stockRequest)
                    println("----")
                    println(stockResponse.status)
                    println(stockResponse.bodyAsText())
                    respondToClient(stockResponse, call)
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)
            }
        }
        route("/Bot") {
            post("/Backtesting") {
                val backtestConfig = call.receive<BacktestConfig>()
                println("--------")
                println(backtestConfig)

                val backtestResult = backtestConfig.let {
                    tradingController.doBacktesting(it.strategySelector, it.stockAggregationRequest)
                }
                println(backtestResult.winRate)
                println(backtestResult.avgProfit)
                println(backtestResult.totalProfit)
                println(backtestResult.strategyName)
                call.respond(HttpStatusCode.OK, backtestResult)
            }

            get("/Start") {
                tradingController.startBot()
                call.respond(HttpStatusCode.OK)
            }
            get("/Stop") {
                tradingController.stopBot()
                call.respond(HttpStatusCode.OK)
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