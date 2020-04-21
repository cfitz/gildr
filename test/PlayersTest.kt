package dog.wildtulsa


import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import java.util.*
import kotlin.test.*


class PlayersTest {

    @Test
    fun `All players must have a name`() {
        assertFailsWith<IllegalArgumentException> { Player(name = "", password = UUID.randomUUID().toString()) }
    }

    @Test
    fun `All players must have a password`() {
        assertFailsWith<IllegalArgumentException> { Player(name = "x", password = "") }
    }

    @Test
    fun `GET players requests without tokens should fail`() {
        withServer {
            handleRequest(HttpMethod.Get, "/v1/players").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `GET players requests with tokens should pass`() {
        withServer {
            handleRequest(HttpMethod.Get, "/v1/players") {
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

/*
    Currently disabled to allow for first time new user creation.
    @Test
    fun `POST requests without tokens should fail`() {
        withServer {
            handleRequest(HttpMethod.Post, "/v1/players").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
*/

    @Test
    fun `POST players request with tokens should pass and create a new user`() {
        val playerPost = PostPlayer(player = PostPlayer.Cred(name = "noob", password = "password123"))
        withServer {
            handleRequest(HttpMethod.Post, "/v1/players") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
                setBody(gson.toJson(playerPost))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
                val json = gson.fromJson(response.content, Player::class.java)
                assertEquals(json.name, playerPost.player.name)
                assertNotNull(json.id)
                assertNotEquals(json.password, playerPost.player.password)
            }
        }
    }

    @Test
    fun `GET player request with tokens should return a fully resolved user`() {

        withServer {
            val player1 = Player(name = "player1", password = "ready")
            val guild1 = Guild(name = "one", description = "desc", ownerIds = listOf(player1.id))
            val guild2 = Guild(
                name = "one",
                description = "desc",
                ownerIds = listOf(UUID.randomUUID()),
                memberIds = listOf(player1.id)
            )
            testStore.add(player1)
            testStore.add(guild2)
            testStore.add(guild1)
            handleRequest(HttpMethod.Get, "/v1/players/${player1.id}") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
                val json = gson.fromJson(response.content, Player::class.java)
                assertEquals(json.name, player1.name)
                assertTrue(json.ownerOf.any { it.id == guild1.id })
                assertTrue(json.memberOf.any { it.id == guild2.id })
            }
        }
    }

}



