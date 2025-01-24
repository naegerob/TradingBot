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
                println(orderRequest)
                val isSuccessfulSet = trader.setOrderParameter(orderRequest)
                if(isSuccessfulSet) {
                    call.respond(orderRequest)
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)
            }
            get("/Create") {
                val request = trader.getOrderRequest()
                println("request")
                println(request)
                val orderResponse = trader.createOrder()
                println("orderResponse")
                println(orderResponse)
                respondToClient(orderResponse, call)
            }
        }
        route("/HistoricalBars") {
            post("/SetAllParameter") {
                val stockRequest = call.receive<StockAggregationRequest>()
                trader.setAggregationParameter(stockRequest)
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
        HttpStatusCode.OK                   -> {
            val details = httpResponse.bodyAsText()
            println(details)
            call.respond(HttpStatusCode.OK, details)
        }
        HttpStatusCode.MovedPermanently     -> call.respond(HttpStatusCode.MovedPermanently)
        HttpStatusCode.NotFound             -> call.respond(HttpStatusCode.NotFound)
        HttpStatusCode.Forbidden            -> call.respond(HttpStatusCode.Forbidden, "Buying power or shares is not sufficient.")
        HttpStatusCode.UnprocessableEntity  -> call.respond(HttpStatusCode.UnprocessableEntity, "Input parameters are not recognized.")
        else                                -> call.respond(HttpStatusCode.InternalServerError, "Error is not handled.")
    }
}