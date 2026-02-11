package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.configuration.loadRSAPrivateKey
import com.example.configuration.loadRSAPublicKey
import com.example.data.singleModels.OrderRequest
import com.example.data.singleModels.StockAggregationRequest
import com.example.data.token.LoginRequest
import com.example.data.token.RefreshRequest
import com.example.tradinglogic.BacktestConfig
import com.example.tradinglogic.Strategies
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthenticationTests {

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = false
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
        install(DefaultRequest) {
            header("content-type", "application/json")
            header("accept", "application/json")
        }
    }

    private enum class HttpVerb { GET, POST }

    private data class ProtectedEndpoint(
        val verb: HttpVerb,
        val path: String,
        val body: Any? = null
    )

    private suspend fun assertUnauthorizedOrForbidden(client: HttpClient, endpoint: ProtectedEndpoint) {
        val response = when (endpoint.verb) {
            HttpVerb.GET -> client.get(endpoint.path)
            HttpVerb.POST -> client.post(endpoint.path) {
                endpoint.body?.let { setBody(it) }
            }
        }

        // Ktor kann je nach Auth-Setup 401 oder 403 liefern.
        val expected = setOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)
        if (response.status !in expected) {
            assertEquals(
                HttpStatusCode.Unauthorized,
                response.status,
                "${endpoint.verb} ${endpoint.path} should return Unauthorized/Forbidden without a token"
            )
        }
    }

    private fun protectedEndpoints(): List<ProtectedEndpoint> {
        val minimalValidOrder = OrderRequest(
            side = "buy",
            type = "market",
            timeInForce = "day",
            quantity = "1",
            notional = null,
            symbol = "AAPL",
            limitPrice = null,
            stopPrice = null,
            trailPrice = null,
            trailPercent = null,
            extendedHours = false,
            clientOrderId = null,
            orderClass = null,
            takeProfit = null,
            stopLoss = null,
            positionIntent = null
        )

        val minimalValidStockRequest = StockAggregationRequest(
            symbols = "AAPL",
            timeframe = "1Min",
            startDateTime = "2024-01-03T00:00:00Z",
            endDateTime = "2024-01-03T01:00:00Z",
            limit = 10
        )

        val minimalBacktestConfig = BacktestConfig(
            strategySelector = Strategies.MovingAverage,
            stockAggregationRequest = minimalValidStockRequest
        )

        val getPaths = listOf(
            "/AccountDetails",
            "/clock",
            "/Indicators/Original",
            "/Indicators/Support",
            "/Indicators/Resistance",
            "/Indicators/Sma/Short",
            "/Indicators/Sma/Long",
            "/Indicators/BollingerBands/Middle",
            "/Indicators/BollingerBands/Upper",
            "/Indicators/BollingerBands/Lower",
            "/Indicators/Rsi",
            "/BacktestIndicators/Original",
            "/BacktestIndicators/Support",
            "/BacktestIndicators/Resistance",
            "/BacktestIndicators/Sma/Short",
            "/BacktestIndicators/Sma/Long",
            "/BacktestIndicators/BollingerBands/Middle",
            "/BacktestIndicators/BollingerBands/Upper",
            "/BacktestIndicators/BollingerBands/Lower",
            "/BacktestIndicators/Rsi",
            "/HistoricalBars/Get",
            "/Bot/Start",
            "/Bot/Stop"
        )

        val postEndpoints = listOf(
            ProtectedEndpoint(HttpVerb.POST, "/Order/Create", minimalValidOrder),
            ProtectedEndpoint(HttpVerb.POST, "/HistoricalBars/Request", minimalValidStockRequest),
            ProtectedEndpoint(HttpVerb.POST, "/Bot/Backtesting", minimalBacktestConfig)
        )

        return getPaths.map { ProtectedEndpoint(HttpVerb.GET, it) } + postEndpoints
    }

    @Test
    fun `All protected endpoints have no valid token`() = testApplication {

        environment { config = ApplicationConfig("application.yaml") }

        val client = createJsonClient()

        protectedEndpoints().forEach { endpoint ->
            assertUnauthorizedOrForbidden(client, endpoint)
        }
    }

    @Test
    fun `Login fails with empty username`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        val response = client.post("/login") {
            setBody(LoginRequest(username = "", password = "somePassword"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Login fails with empty password`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        val response = client.post("/login") {
            setBody(LoginRequest(username = "someUser", password = ""))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Login fails with whitespace username`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        val response = client.post("/login") {
            setBody(LoginRequest(username = "   ", password = "somePassword"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Login fails with whitespace password`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        val response = client.post("/login") {
            setBody(LoginRequest(username = "someUser", password = "   "))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Login fails when username field is missing`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        // send raw json without username; should not authenticate
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{\"password\":\"somePassword\"}""")
        }

        // Depending on deserialization settings this might be 400; but it must never be 200.
        assert(response.status != HttpStatusCode.OK) {
            "Expected /login to not succeed when username is missing, but got ${response.status}"
        }
    }

    @Test
    fun `Login fails when password field is missing`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{\"username\":\"someUser\"}""")
        }

        // Depending on deserialization settings this might be 400; but it must never be 200.
        assert(response.status != HttpStatusCode.OK) {
            "Expected /login to not succeed when password is missing, but got ${response.status}"
        }
    }

    private fun createExpiredAccessToken(): String {
        val appConfig = ApplicationConfig("application.yaml")
        val jwtConfig = appConfig.config("jwt")
        val issuer = jwtConfig.property("issuer").getString()
        val audience = jwtConfig.property("audience").getString()
        val privateKeyPath = jwtConfig.property("privateKeyPath").getString()
        val publicKeyPath = jwtConfig.property("publicKeyPath").getString()

        val privateKey = loadRSAPrivateKey(privateKeyPath)
        val publicKey = loadRSAPublicKey(publicKeyPath)

        val now = System.currentTimeMillis()
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withSubject("test-user")
            .withClaim("username", "test-user")
            .withClaim("type", "access")
            .withIssuedAt(java.util.Date(now - 60_000))
            .withExpiresAt(java.util.Date(now - 1_000)) // already expired
            .sign(Algorithm.RSA256(publicKey, privateKey))
    }


    @Test
    fun `Refresh endpoint rejects invalid refresh token immediately`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        val response = client.post("/auth/refresh") {
            setBody(RefreshRequest(refreshToken = "invalid.refresh.token"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Refresh flow works without waiting for access token to expire`() = testApplication {
        environment { config = ApplicationConfig("application.yaml") }
        val client = createJsonClient()

        // login (works only if AUTHENTIFICATION_USERNAME/PASSWORD env vars are set in test env)
        val username = System.getenv("AUTHENTIFICATION_USERNAME") ?: ""
        val password = System.getenv("AUTHENTIFICATION_PASSWORD") ?: ""

        // If credentials are not available, this test can't be meaningfully executed; don't fail the suite.
        if (username.isBlank() || password.isBlank()) return@testApplication

        val loginResponse = client.post("/login") {
            setBody(LoginRequest(username = username, password = password))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val loginBody = loginResponse.body<Map<String, String>>()
        val refreshToken = loginBody["refreshToken"]
        assertNotNull(refreshToken)

        val refreshResponse = client.post("/auth/refresh") {
            setBody(RefreshRequest(refreshToken = refreshToken))
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)

        val refreshBody = refreshResponse.body<Map<String, String>>()
        val newAccessToken = refreshBody["accessToken"]
        assertNotNull(newAccessToken)
        assert(newAccessToken.count { it == '.' } == 2) { "Expected JWT format for accessToken" }
    }
}
