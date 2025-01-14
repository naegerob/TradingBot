package com.example

import com.example.tradinglogic.TradingLogic
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.finance.datamodel.OrderRequest
import org.example.finance.datamodel.timeInForce

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
                trader.createOrder()
                call.respond("orderRequest")
            }
            post("/SetAllParameter") {
                val orderRequest = call.receive<OrderRequest>()
                trader.setOrderParameter(orderRequest)
                call.respond(orderRequest)
            }
        }
    }
}
