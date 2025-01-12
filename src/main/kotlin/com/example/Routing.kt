package com.example

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.finance.datamodel.AlpacaAPI

fun Application.configureRouting() {
    routing {

        /************************************************************
            GET-Requests (data are sent to client)
        ************************************************************/
        get("/") {
            call.respondText("Hello World!")
        }

        get("/account") {
            val alpaca = AlpacaAPI()
            val accountDetails = alpaca.getAccountDetails()
            call.respondText(accountDetails)
        }
        /************************************************************
            POST-Requests (data are sent from client)
        ************************************************************/
        post("/text") {
            val text = call.receiveText()
            call.respondText(text)
        }

    }
}
