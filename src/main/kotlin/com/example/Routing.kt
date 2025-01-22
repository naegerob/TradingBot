package com.example

import com.example.tradingLogic.TradingLogic
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.finance.datamodel.*

fun Application.configureRouting(trader: TradingLogic) {

    routing {

        get("/AccountDetails") {
            val accountResponse = trader.fetchAccountDetails() // Assuming this returns an HttpResponse
            respondToClient<Account>(accountResponse, call)
        }

        route("/Order") {
            post("/SetAllParameter") {
                val orderRequest = call.receive<OrderRequest>()
                println(orderRequest)
                val isSuccessfulSet = trader.setOrderParameter(orderRequest)
                if(isSuccessfulSet) {
                    call.respond(orderRequest)
                }
                call.respond(HttpStatusCode.BadRequest)
            }
            get("/Create") {
                val orderResponse = trader.createOrder()
                respondToClient<OrderResponse>(orderResponse, call)
            }
        }
        route("/HistoricalBars") {
            post("/SetAllParameter") {
                val stockRequest = call.receive<StockAggregationRequest>()
                trader.setAggregationParameter(stockRequest)
            }

            get("/Request") {
                val historicalBarsResponse = trader.getHistoricalBars()
                respondToClient<StockAggregationResponse>(historicalBarsResponse, call)
            }
        }


        
    }
}

suspend fun <T>respondToClient(httpResponse: HttpResponse, call: RoutingCall) {

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