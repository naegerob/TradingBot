package com.example

import com.example.tradingLogic.TradingLogic
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.finance.datamodel.OrderRequest

fun Application.configureRouting(trader: TradingLogic) {

    routing {

        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/accountDetails") {
            val accountDetails = trader.fetchAccountDetails()
            call.respond(accountDetails)
        }

        route("/Order") {
            get("/Create") {
                val orderResponse = trader.createOrder()
                requireNotNull(orderResponse)
                call.respond(orderResponse)
            }
            post("/SetAllParameter") {
                val orderRequest = call.receive<OrderRequest>()
                trader.setOrderParameter(orderRequest)
                call.respond(orderRequest)
            }
        }
    }
}
