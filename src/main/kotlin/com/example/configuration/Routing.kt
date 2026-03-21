package com.example.configuration

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.database.DataBaseFacade
import com.example.data.database.DataBaseImpl
import com.example.services.ValidationService
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.singleModels.LoginRequest
import com.example.data.singleModels.RefreshRequest
import com.example.tradinglogic.BacktestConfig
import com.example.tradinglogic.BotConfig
import com.example.tradinglogic.Result
import com.example.tradinglogic.TradingController
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*


fun Application.configureRouting() {
    val tradingController = TradingController()
    val validator = ValidationService()

    val jwtConfig = environment.config.config("jwt")
    val issuer = jwtConfig.property("issuer").getString()
    val audience = jwtConfig.property("audience").getString()
    val privateKeyPath = jwtConfig.property("privateKeyPath").getString()
    val publicKeyPath = jwtConfig.property("publicKeyPath").getString()
    val privateKey by lazy { loadRSAPrivateKey(privateKeyPath) }
    val publicKey by lazy { loadRSAPublicKey(publicKeyPath) }
    val algorithm by lazy { Algorithm.RSA256(publicKey, privateKey) }
    val verifier by lazy {
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }
    val db: DataBaseFacade = DataBaseImpl()

    fun issueAccessToken(username: String, nowMs: Long): String {
        val tokenIdentifier = UUID.randomUUID().toString()
        val numberOfMinutes = 120L //15L
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withSubject(username)
            .withClaim("username", username)
            .withClaim("type", "access")
            .withIssuedAt(Date(nowMs))
            .withExpiresAt(Date(nowMs + numberOfMinutes * 60 * 1000L))
            .withJWTId(tokenIdentifier)
            .sign(algorithm)
    }

    fun issueRefreshToken(username: String, nowMs: Long): Pair<String, String> {
        val tokenIdentifier = UUID.randomUUID().toString()
        val numberOfDays = 7L
        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withSubject(username)
            .withClaim("username", username)
            .withClaim("type", "refresh")
            .withIssuedAt(Date(nowMs))
            .withExpiresAt(Date(nowMs + numberOfDays * 24 * 60 * 60 * 1000))
            .withJWTId(tokenIdentifier)
            .sign(algorithm)
        return token to tokenIdentifier
    }

    suspend fun validateRefreshTokenOrRespond(
        call: ApplicationCall,
        verifier: JWTVerifier,
        refreshToken: String
    ): Pair<String, String>? {
        return try {
            val decodedJWT = verifier.verify(refreshToken)
            val type = decodedJWT.getClaim("type")?.asString()
            if (type != "refresh") {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "INVALID_TOKEN_TYPE"))
                return null
            }
            val refreshTokenId = decodedJWT.id
            if (refreshTokenId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "MISSING_TOKEN_ID"))
                return null
            }
            val username = decodedJWT.subject
            if (username.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "MISSING_SUBJECT"))
                return null
            }
            username to refreshTokenId
        } catch (_: Exception) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "INVALID_REFRESH_TOKEN"))
            null
        }
    }

    routing {
        post("/") {
            call.respondRedirect("/login")
        }
        rateLimit(RateLimitName("login")) {
            post("/login") {
                val loginRequest = call.receive<LoginRequest>()

                val password = System.getenv("AUTHENTIFICATION_PASSWORD")
                val username = System.getenv("AUTHENTIFICATION_USERNAME")

                if (loginRequest.username == username && loginRequest.password == password) {
                    val now = System.currentTimeMillis()
                    val (refreshToken, refreshTokenId) = issueRefreshToken(loginRequest.username, now)
                    db.addToken(refreshTokenId, refreshToken)
                    val accessToken = issueAccessToken(loginRequest.username, now)
                    call.respond(
                        mapOf(
                            "accessToken" to accessToken,
                            "refreshToken" to refreshToken
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            }
        }
        rateLimit(RateLimitName("login")) {
            post("/auth/refresh") {
                val refreshTokenRequest = call.receive<RefreshRequest>()
                if (refreshTokenRequest.refreshToken.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "MISSING_REFRESH_TOKEN"))
                    return@post
                }

                val (username, refreshTokenId) = validateRefreshTokenOrRespond(call, verifier, refreshTokenRequest.refreshToken) ?: return@post
                val doesTokenAlreadyExist = db.doesTokenExist(refreshTokenId)
                if (!doesTokenAlreadyExist) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "REFRESH_TOKEN_NOT_FOUND"))
                    return@post
                }

                val now = System.currentTimeMillis()
                val newAccessToken = issueAccessToken(username, now)

                val remainingMs = (verifier.verify(refreshTokenRequest.refreshToken).expiresAt?.time ?: 0L) - now
                val rotateThresholdMs = 24L * 60 * 60 * 1000

                if (remainingMs < rotateThresholdMs) {
                    val (newRefreshToken, newRefreshTokenId) = issueRefreshToken(username, now)
                    db.deleteToken(refreshTokenId)
                    db.addToken(newRefreshTokenId, newRefreshToken)
                    call.respond(
                        mapOf(
                            "accessToken" to newAccessToken,
                            "refreshToken" to newRefreshToken,
                            "rotated" to "true"
                        )
                    )
                } else {
                    call.respond(
                        mapOf(
                            "accessToken" to newAccessToken,
                            "refreshToken" to refreshTokenRequest.refreshToken,
                            "rotated" to "false"
                        )
                    )
                }
            }
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, "Server is healthy")
        }
        post("/logout") {
            val refreshTokenRequest = call.receive<RefreshRequest>()
            if (refreshTokenRequest.refreshToken.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "MISSING_REFRESH_TOKEN"))
                return@post
            }
            val (_, refreshTokenId) = validateRefreshTokenOrRespond(call, verifier, refreshTokenRequest.refreshToken)
                ?: return@post

            if (db.doesTokenExist(refreshTokenId)) {
                db.deleteToken(refreshTokenId)
                call.respond(HttpStatusCode.OK, "Logged out successfully")
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "REFRESH_TOKEN_NOT_FOUND"))
            }
        }

        authenticate("auth-jwt") {
            get("/AccountDetails") {
                val accountResponse = tradingController.fetchAccountDetails()
                respondToClient(accountResponse, call)
            }
            get("/clock") {
                val openingHoursResponse = tradingController.getOpeningHours()
                respondToClient(openingHoursResponse, call)
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
                    val isValidRequest = validator.areValidOrderParameter(orderRequest)
                    if (isValidRequest) {
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
                    log.info("Received bars request in ${call.request.path()}: $stockRequest")
                    val isSuccessfulSet = validator.areValidStockRequestParameter(stockRequest)
                    if (isSuccessfulSet) {
                        val stockResponse = tradingController.getStockData(stockRequest)
                        respondToClient(stockResponse, call)
                        return@get
                    }
                    call.respond(HttpStatusCode.BadRequest)
                }
                post("/Request") {
                    val stockRequest = call.receive<StockAggregationRequest>()
                    log.info("Received stock request in ${call.request.path()}: $stockRequest")
                    val isSuccessfulSet = validator.areValidStockRequestParameter(stockRequest)
                    if (isSuccessfulSet) {
                        val stockResponse = tradingController.getStockData(stockRequest)
                        respondToClient(stockResponse, call)
                        return@post
                    }
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            route("/Bot") {
                post("/Backtesting") {
                    val backtestConfig = call.receive<BacktestConfig>()
                    log.info("Backtesting: $backtestConfig")
                    when (val backtestResult = tradingController.doBacktesting(backtestConfig)) {
                        is Result.Error -> return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "something went wrong in backTesting, check your config"
                        )
                        is Result.Success -> return@post call.respond(HttpStatusCode.OK, backtestResult.data)
                    }
                }
                post("/Config") {
                    if(tradingController.isBotRunning()) {
                        call.respond(HttpStatusCode.BadRequest, "Cannot update config while bot is running. Please stop the bot first.")
                        return@post
                    }
                    val botConfig = call.receive<BotConfig>()
                    log.info("Received bot config: $botConfig")
                    val isValidConfig = validator.isValidBotConfig(botConfig)
                    if (isValidConfig) {
                        tradingController.setBotConfig(botConfig)
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid bot configuration parameters.")
                    }
                }
                get("/Start") {
                    if(tradingController.isBotRunning()) {
                        call.respond(HttpStatusCode.BadRequest, "Bot is already running.")
                        return@get
                    }
                    if(!tradingController.isBotConfigured()) {
                        call.respond(HttpStatusCode.BadRequest, "Bot is not configured yet.")
                        return@get
                    }
                    launch{ tradingController.startBot() }
                    call.respond(HttpStatusCode.OK)
                }
                get("/Stop") {
                    if(!tradingController.isBotRunning()) {
                        call.respond(HttpStatusCode.BadRequest, "Bot was already stoppped.")
                        return@get
                    }
                    tradingController.stopBot()
                    call.respond(HttpStatusCode.OK)
                }
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

suspend fun respondToClient(httpResponse: HttpResponse, call: RoutingCall) {
    when (httpResponse.status) {
        HttpStatusCode.OK -> {
            val body = httpResponse.bodyAsText()
            call.respondText(body, ContentType.Application.Json, HttpStatusCode.OK)
        }
        HttpStatusCode.BadRequest -> call.respond(
            HttpStatusCode.BadRequest,
            "Parameter have wrong format. Check Alpaca Doc!"
        )
        HttpStatusCode.MovedPermanently -> call.respond(HttpStatusCode.MovedPermanently)
        HttpStatusCode.NotFound -> call.respond(HttpStatusCode.NotFound)
        HttpStatusCode.Forbidden -> call.respond(
            HttpStatusCode.Forbidden,
            "Buying power or shares is not sufficient. Or proxy blocks API call."
        )
        HttpStatusCode.UnprocessableEntity -> call.respond(
            HttpStatusCode.UnprocessableEntity,
            "Input parameters are not recognized. Or Alpaca has closed"
        )
        else -> call.respond(HttpStatusCode.InternalServerError, "Error is not handled.")
    }
}
