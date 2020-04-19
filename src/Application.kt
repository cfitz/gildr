
package dog.wildtulsa

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.http.content.*
import io.ktor.features.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import io.ktor.auth.jwt.jwt
import io.ktor.jackson.*
import io.ktor.util.getDigestFunction
import java.io.File
import java.io.IOException
import java.util.*


@Location("/v1/players")
class Players()

@Location( "/v1/guilds")
class Guilds(val q: String = "")

@Location( "/v1/invites")
class Invites()

@Location("/v1/players/{id}")
data class Profile(val id: String)

@Location("/v1/guilds/{id}")
data class GuildProfile( val id: String)

@Location("/v1/guilds/{id}/membership")
data class GuildMembership( val id: String)

@Location("/v1/guilds/{id}/join")
data class GuildJoin(val id: String )

@Location("/v1/guilds/{id}/leave")
data class GuildLeave(val id: String )


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

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
    }

    install(Locations)

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    fun makeDataDirs(dirPath: String ) {
        val dir = File(dirPath)
        if (!dir.mkdirs() && !dir.exists()) {
            throw IOException("Failed to create directory ${dir.absolutePath}")
        }
    }

    val appConfig = environment.config.config("gildr")
    // make sure our data directories are there for our json files..
    val dataDirPath: String = appConfig.property("data.dir").getString()
    listOf( dataDirPath, "${dataDirPath}/players", "${dataDirPath}/guilds", "${dataDirPath}/invites").forEach { makeDataDirs(it) }


    val playerStore = PlayerStore(File("${dataDirPath}/players"))
    val guildStore = GuildStore(File("${dataDirPath}/guilds"))
    val inviteStore = InviteStore(File("${dataDirPath}/invites"))

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        post("/login") {
            val post = call.receive<UserPasswordCredential>()
            val users = playerStore.listAll().map { User(it.id, it.name, Base64.getDecoder().decode(it.password))}.toList()
            val hashedUserTable = UserHashedTableAuth(
                    getDigestFunction("SHA-256") { "ktor${it.length}" },
                    table = users.associateBy({it.name}, {it.password}))
            val user = hashedUserTable.authenticate(UserPasswordCredential(post.name, post.password))
                ?: throw InvalidCredentialsException("Invalid credentials")
            // actually redo the auth so we dont have to do this.
            // would want to put the actual play in the claim
            val player = users.find { it.name == user.name }
            if (player != null) {
                call.respond(mapOf("token" to simpleJwt.sign(player.id.toString())))
            }
        }

        players(playerStore)
        guilds(guildStore)
        invites(inviteStore)

        playerProfile(playerStore, guildStore, inviteStore)
        guildProfile(playerStore, guildStore, inviteStore)

        guildMembership(guildStore)

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

    }
}

class User(val id: UUID, val name: String, val password: ByteArray)
class InvalidCredentialsException(message: String) : RuntimeException(message)
