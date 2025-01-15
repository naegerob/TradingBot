package com.example

import com.example.tradingLogic.TradingLogic
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
                trader.setOrderParameter(orderRequest)
                call.respond(orderRequest)
            }
            get("/Create") {
                val orderResponse = trader.createOrder()
                requireNotNull(orderResponse)
                call.respond(orderResponse)
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
                requireNotNull(historicalBarsResponse)
                call.respond(historicalBarsResponse)
            }
        }
        
    }
}
