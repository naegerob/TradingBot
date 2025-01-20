package com.example

import com.example.tradingLogic.TradingLogic
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.finance.datamodel.OrderRequest
import org.example.finance.datamodel.StockAggregationRequest

fun Application.configureRouting(trader: TradingLogic) {

    routing {

        get("/AccountDetails") {
            val accountDetails = trader.fetchAccountDetails()
            call.respond(accountDetails)
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
                call.respond((HttpStatusCode.OK))
                return@get
                val orderResponse = trader.createOrder()
                when(orderResponse.status) {
                    HttpStatusCode.OK -> call.respond(orderResponse)
                    HttpStatusCode.UnprocessableEntity -> call.respond(HttpStatusCode.UnprocessableEntity)
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }

            }
        }
        route("/HistoricalBars") {
            post("/SetAllParameter") {
                val stockRequest = call.receive<StockAggregationRequest>()
                trader.setAggregationParameter(stockRequest)
                call.respond(stockRequest)
            }

            get("/Request") {
                val historicalBarsResponse = trader.getHistoricalBars()
                call.respond(historicalBarsResponse)
            }
        }
        
    }
}
