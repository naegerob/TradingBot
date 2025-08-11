package com.example.tradingLogic


fun Application.configureAuthentification() {
    install(Authentication) {
        jwt {
            // Configure jwt authentication
        }
    }
}