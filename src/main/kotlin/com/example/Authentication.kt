import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.concurrent.TimeUnit

fun Application.configureAuthentication() {
    val jwtConfig = environment.config.config("jwt")

    val issuer = jwtConfig.property("issuer").getString()
    val audience = jwtConfig.property("audience").getString()
    val myRealm = jwtConfig.property("realm").getString()

    val jwkProvider = JwkProviderBuilder(issuer)    // fetches public key dynamically
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(jwkProvider, issuer) {
                acceptLeeway(3) // allow 3 seconds clock skew
            }
            validate { credential ->
                if (credential.payload.audience.contains(audience))
                    JWTPrincipal(credential.payload)
                else
                    null
            }
            challenge { _,_ ->
                call.respondText("Token is not valid or has expired", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}


