package dog.wildtulsa

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserHashedTableAuth
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.getDigestFunction
import java.io.File
import java.util.*


@Location("/v1/players")
class Players

@Location("/v1/players/{id}")
data class Profile(val id: String)

@Location("/v1/guilds")
class Guilds(val q: String = "")

@Location("/v1/guilds/{id}")
data class GuildProfile(val id: String)

@Location("/v1/guilds/{id}/admin")
data class GuildAdmin(val id: String)

@Location("/v1/guilds/{id}/join")
data class GuildJoin(val id: String)

@Location("/v1/guilds/{id}/leave")
data class GuildLeave(val id: String)

// @Location( "/v1/invites")
// class Invites()


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}


val digestFn = getDigestFunction("SHA-256") { "ktor${it.length}" }
fun hash(password: String): String {
    val digest = digestFn(password)
    return Base64.getEncoder().encodeToString(digest)
}

val hashFn = { s: String -> hash(s) }

val dataDir = File(".data")

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(
    testing: Boolean = false,
    store: DAOFacadeCache = DAOFacadeCache(DAOFacadeStore(dataDir), File(dataDir, "cache"))
) {

    install(CallLogging)
    install(ForwardedHeaderSupport)
    install(DefaultHeaders)

    install(Locations)

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.AccessControlRequestHeaders)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        anyHost()
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    val appConfig = environment.config.config("gildr")

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")
    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    install(StatusPages) {
        exception<InvalidCredentialsException> { exception ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }

        exception<IllegalArgumentException> { exception ->
            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }

        exception<InvalidFormatException> { exception ->
            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }


        exception<MissingKotlinParameterException> { exception ->
            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }

        status(HttpStatusCode.Unauthorized) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to "NOT ALLOWED"))
        }
    }


    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }



    routing {

        static("/") {
            resources("static")
            default("./static/index.html")
            resource("/", "./static/index.html")
            resource("*", "./static/index.html")
        }

        static("/static") {
            resources("static")
        }

        post("/login") {
            val post = call.receive<UserPasswordCredential>()
            val users =
                store.listAllPlayers().map { User(it.id, it.name, Base64.getDecoder().decode(it.password)) }.toList()
            val hashedUserTable = UserHashedTableAuth(
                digestFn,
                table = users.associateBy({ it.name }, { it.password })
            )
            val user = hashedUserTable.authenticate(UserPasswordCredential(post.name, post.password))
                ?: throw InvalidCredentialsException("Invalid credentials")
            // actually redo the auth so we dont have to do this.
            // would want to put the actual play in the claim
            val player = users.find { it.name == user.name }
            if (player != null) {
                call.respond(mapOf("token" to simpleJwt.sign(player.id.toString())))
            }
        }

        players(store, hashFn)
        guilds(store)
        // invites(inviteStore)

        // guildMembership(guildStore)


    }
}

class User(val id: UUID, val name: String, val password: ByteArray)
class InvalidCredentialsException(message: String) : RuntimeException(message)
