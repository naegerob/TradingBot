package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.tradingLogic.BacktestConfig
import com.example.tradingLogic.TradingController
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

fun Application.configureRouting() {
    val tradingController = TradingController()
    routing {
        authenticate("auth-jwt") {
            get("/") {
                call.respondText("Hi")
            }
        }

        // 🔑 Login route
        get("/login") {

            val tempLoginRequest = LoginRequest(
                username = "admin",
                password = "password"
            )
            val loginRequest = tempLoginRequest.copy()

            val jwtConfig = environment.config.config("jwt")

            val issuer = jwtConfig.property("issuer").getString()
            val audience = jwtConfig.property("audience").getString()

            val privateKeyPath = jwtConfig.property("privateKeyPath").getString()
            val publicKeyPath = jwtConfig.property("publicKeyPath").getString()

            val privateKey = loadRSAPrivateKey(privateKeyPath)
            val publicKey = loadRSAPublicKey(publicKeyPath)

            val password = System.getenv("AUTHENTIFICATION_PASSWORD")

            if (loginRequest.username == "admin" && loginRequest.password == "password") {
                println("We're in!")
                val token = JWT.create()
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("username", loginRequest.username)
                    .withExpiresAt(Date(System.currentTimeMillis() + 1200000)) // 20 min expiry
                    .sign(Algorithm.RSA256(publicKey, privateKey))


                call.respond(mapOf("token" to token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }

        get("/AccountDetails") {
            val accountResponse = tradingController.fetchAccountDetails()
            respondToClient(accountResponse, call)
        }
        route("/Indicators") {
            get("/Original") {
                val original = tradingController.mTradingBot.mIndicators.mOriginalPrices
                call.respondText(original.toString(), status = HttpStatusCode.OK)
            }
            get("/Support") {
                val support = tradingController.mTradingBot.mIndicators.mSupports
                call.respondText(support.toString(), status = HttpStatusCode.OK)
            }
            get("/Resistance") {
                val resistance = tradingController.mTradingBot.mIndicators.mResistances
                call.respondText(resistance.toString(), status = HttpStatusCode.OK)
            }
            route("/Sma") {
                get("/Short") {
                    val smaShort = tradingController.mTradingBot.mIndicators.mShortSMA
                    call.respondText(smaShort.toString(), status = HttpStatusCode.OK)
                }
                get("/Long") {
                    val smaLong = tradingController.mTradingBot.mIndicators.mLongSMA
                    call.respondText(smaLong.toString(), status = HttpStatusCode.OK)
                }
            }
            route("/BollingerBands") {
                get("/Middle") {
                    val sma = tradingController.mTradingBot.mIndicators.mAverageBollingerBand
                    call.respondText(sma.toString(), status = HttpStatusCode.OK)
                }
                get("/Upper") {
                    val upperBollinger = tradingController.mTradingBot.mIndicators.mUpperBollingerBand
                    call.respondText(upperBollinger.toString(), status = HttpStatusCode.OK)
                }
                get("/Lower") {
                    val lowerBollinger = tradingController.mTradingBot.mIndicators.mLowerBollingerBand
                    call.respondText(lowerBollinger.toString(), status = HttpStatusCode.OK)
                }
            }
            get("/Rsi") {
                val rsi = tradingController.mTradingBot.mIndicators.mRsi
                call.respondText(rsi.toString(), status = HttpStatusCode.OK)
            }
        }
        route("/BacktestIndicators") {
            get("/Original") {
                val original = tradingController.mTradingBot.mBacktestIndicators.mOriginalPrices
                call.respondText(original.toString(), status = HttpStatusCode.OK)
            }
            get("/Support") {
                val support = tradingController.mTradingBot.mBacktestIndicators.mSupports
                call.respondText(support.toString(), status = HttpStatusCode.OK)
            }
            get("/Resistance") {
                val resistance = tradingController.mTradingBot.mBacktestIndicators.mResistances
                call.respondText(resistance.toString(), status = HttpStatusCode.OK)
            }
            route("/Sma") {
                get("/Short") {
                    val smaShort = tradingController.mTradingBot.mBacktestIndicators.mShortSMA
                    call.respondText(smaShort.toString(), status = HttpStatusCode.OK)
                }
                get("/Long") {
                    val smaLong = tradingController.mTradingBot.mBacktestIndicators.mLongSMA
                    call.respondText(smaLong.toString(), status = HttpStatusCode.OK)
                }
            }
            route("/BollingerBands") {
                get("/Middle") {
                    val sma = tradingController.mTradingBot.mBacktestIndicators.mAverageBollingerBand
                    call.respondText(sma.toString(), status = HttpStatusCode.OK)
                }
                get("/Upper") {
                    val upperBollinger = tradingController.mTradingBot.mBacktestIndicators.mUpperBollingerBand
                    call.respondText(upperBollinger.toString(), status = HttpStatusCode.OK)
                }
                get("/Lower") {
                    val lowerBollinger = tradingController.mTradingBot.mBacktestIndicators.mLowerBollingerBand
                    call.respondText(lowerBollinger.toString(), status = HttpStatusCode.OK)
                }
            }
            get("/Rsi") {
                val rsi = tradingController.mTradingBot.mBacktestIndicators.mRsi
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
            get("/Get") {
                val stockRequest = StockAggregationRequest()
                println(stockRequest)
                val isSuccessfulSet = tradingController.areValidStockRequestParameter(stockRequest)
                if(isSuccessfulSet) {
                    val stockResponse = tradingController.getStockData(stockRequest)
                    respondToClient(stockResponse, call)
                    call.respond(stockResponse)
                    return@get
                }
                call.respond(HttpStatusCode.BadRequest)
            }
            post("/Request") {
                val stockRequest = call.receive<StockAggregationRequest>()
                println(stockRequest)
                val isSuccessfulSet = tradingController.areValidStockRequestParameter(stockRequest)
                if(isSuccessfulSet) {
                    val stockResponse = tradingController.getStockData(stockRequest)
                    respondToClient(stockResponse, call)
                    call.respond(stockResponse)
                    return@post
                }
                call.respond(HttpStatusCode.BadRequest)
            }
        }
        route("/Bot") {
            post("/Backtesting") {
                val backtestConfig = call.receive<BacktestConfig>()
                println(backtestConfig)
                val backtestResult = backtestConfig.let {
                    tradingController.doBacktesting(it.strategySelector, it.stockAggregationRequest)
                }
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

fun loadRSAPrivateKey(path: String): RSAPrivateKey {
    val keyBytes = Files.readAllBytes(Paths.get(path))
    val keyPem = String(keyBytes)
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")
    val decoded = Base64.getDecoder().decode(keyPem)
    val spec = PKCS8EncodedKeySpec(decoded)
    return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
}

fun loadRSAPublicKey(path: String): RSAPublicKey {
    val keyBytes = Files.readAllBytes(Paths.get(path))
    val keyPem = String(keyBytes)
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s".toRegex(), "")
    val decoded = Base64.getDecoder().decode(keyPem)
    val spec = X509EncodedKeySpec(decoded)
    return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
}

@Serializable
data class LoginRequest(var username: String, var password: String)

suspend fun respondToClient(httpResponse: HttpResponse, call: RoutingCall) {

    when (httpResponse.status) {
        HttpStatusCode.OK                   -> call.respond(HttpStatusCode.OK, httpResponse.bodyAsText())
        HttpStatusCode.BadRequest           -> call.respond(HttpStatusCode.BadRequest, "Parameter have wrong format. Check Alpaca Doc!")
        HttpStatusCode.MovedPermanently     -> call.respond(HttpStatusCode.MovedPermanently)
        HttpStatusCode.NotFound             -> call.respond(HttpStatusCode.NotFound)
        HttpStatusCode.Forbidden            -> call.respond(HttpStatusCode.Forbidden, "Buying power or shares is not sufficient. Or proxy blocks API call.")
        HttpStatusCode.UnprocessableEntity  -> call.respond(HttpStatusCode.UnprocessableEntity, "Input parameters are not recognized. Or Alpaca has closed")
        else                                -> call.respond(HttpStatusCode.InternalServerError, "Error is not handled.")
    }
}