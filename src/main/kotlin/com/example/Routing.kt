package com.example

import com.example.data.OrderRequest
import com.example.data.StockAggregationRequest
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
            val accountResponse = trader.fetchAccountDetails() // Assuming this returns an HttpResponse
            respondToClient(accountResponse, call)
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
        HttpStatusCode.BadRequest           -> call.respond(HttpStatusCode.BadRequest, "Parameter have wrong format. CHeck Alpaca Doc!")
        HttpStatusCode.MovedPermanently     -> call.respond(HttpStatusCode.MovedPermanently)
        HttpStatusCode.NotFound             -> call.respond(HttpStatusCode.NotFound)
        HttpStatusCode.Forbidden            -> call.respond(HttpStatusCode.Forbidden, "Buying power or shares is not sufficient.")
        HttpStatusCode.UnprocessableEntity  -> call.respond(HttpStatusCode.UnprocessableEntity, "Input parameters are not recognized.")
        else                                -> call.respond(HttpStatusCode.InternalServerError, "Error is not handled.")
    }
}