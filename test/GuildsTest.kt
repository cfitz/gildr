package dog.wildtulsa


import com.google.gson.Gson
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.setBody
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


val gson = Gson()

class GuildsTest {
    @Test
    fun `requests without tokens should fail`() {
        withServer {
            handleRequest(HttpMethod.Get, "/v1/guilds").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `requests with tokens should pass`() {
        withServer {
            handleRequest(HttpMethod.Get, "/v1/guilds") {
               addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun `POST requests without tokens should fail`() {
        withServer {
            handleRequest(HttpMethod.Post, "/v1/guilds").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `POST request with tokens should pass and create a new guild`() {
        val newGuild = Guild(name = "test guild", description = "this is a test", ownerIds = listOf(UUID.randomUUID()), memberIds = listOf()  )
        val guildPost = PostGuild(guild = newGuild)
        withServer {
            handleRequest(HttpMethod.Post, "/v1/guilds") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
                setBody(gson.toJson(guildPost))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
                val json = gson.fromJson(response.content, Guild::class.java)
                assertEquals(json.name, newGuild.name)
                assertNotNull(json.id)
            }
        }
    }


}

