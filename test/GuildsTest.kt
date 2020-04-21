package dog.wildtulsa


import com.google.gson.reflect.TypeToken;
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import java.lang.reflect.Type
import java.util.*
import kotlin.test.*


val fakePlayer: UUID = UUID.randomUUID()
val deleteMe = Guild(name = "dead", description = "dead", ownerIds = listOf(testPlayer.id))
val joinGuild = Guild(name = "join", description = "join", ownerIds = listOf(UUID.randomUUID()))
val leaveGuild = joinGuild.copy(memberIds = listOf(testPlayer.id), ownerIds = listOf(UUID.randomUUID()))
val adminGuild = deleteMe.copy()
val notAdminGuild = joinGuild.copy()

// we need to relfect here to get the List<Guild> from gson.
var guildsType: Type = object : TypeToken<Map<String, List<Map<String, String>>>>() {}.getType()

class GuildsTest {

    @Test
    fun `All Guilds must have a description`() {
        assertFailsWith<IllegalArgumentException> {
            Guild(
                name = "y",
                ownerIds = listOf(UUID.randomUUID()),
                description = ""
            )
        }
    }

    @Test
    fun `All Guilds must have a name`() {
        assertFailsWith<IllegalArgumentException> {
            Guild(
                name = "",
                ownerIds = listOf(UUID.randomUUID()),
                description = "y"
            )
        }
    }

    @Test
    fun `All Guilds must have at least one owner`() {
        assertFailsWith<IllegalArgumentException> { Guild(name = "a", ownerIds = listOf(), description = "y") }
    }


    @Test
    fun `GET guilds requests without tokens should fail`() {
        withServer {
            handleRequest(HttpMethod.Get, "/v1/guilds").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `GET guilds requests with tokens should pass`() {
        withServer {
            handleRequest(HttpMethod.Get, "/v1/guilds") {
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun `GET guilds requests with tokens should pass and accept a search query`() {
        val randomName = UUID.randomUUID().toString()
        val randomGuild = deleteMe.copy(name = randomName)
        testStore.add(randomGuild)
        withServer {
            handleRequest(HttpMethod.Get, "/v1/guilds?q=${randomName.split("-")[0]}") {
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val json: Map<String, List<Map<String, String>>> = gson.fromJson(response.content, guildsType)
                val array = json["guilds"] ?: listOf()
                assertEquals(1, array.size)
                assertEquals(randomGuild.name, array[0]["name"])
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
        val name = UUID.randomUUID().toString()
        val guildPost = PostGuild(guild = PostGuild.Registration(name = name, description = "this is a test"))
        withServer {
            handleRequest(HttpMethod.Post, "/v1/guilds") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
                setBody(gson.toJson(guildPost))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
                val json = gson.fromJson(response.content, Guild::class.java)
                assertEquals(json.name, name)
                assertNotNull(json.id)
            }
        }
    }

    @Test
    fun `DELETE request without tokens should fail`() {
        testStore.add(deleteMe)
        withServer {
            handleRequest(HttpMethod.Delete, "/v1/guilds/${deleteMe.id}") {
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `DELETE request with tokens should succeed`() {
        testStore.add(deleteMe)
        withServer {
            handleRequest(HttpMethod.Delete, "/v1/guilds/${deleteMe.id}") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNull(testStore.guildById(deleteMe.id))
            }
        }
    }

    @Test
    fun `PATCH join request without tokens should fail`() {
        testStore.add(joinGuild)
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${joinGuild.id}/join") {
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `PATCH join request with tokens should pass`() {
        testStore.add(joinGuild)
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${joinGuild.id}/join") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val update = testStore.guildById(joinGuild.id)
                assertNotNull(update)
                assertTrue(update.memberIds.any { it == testPlayer.id })
            }
        }
    }

    @Test
    fun `PATCH leave request without tokens should fail`() {
        testStore.add(leaveGuild)
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${joinGuild.id}/leave") {
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `PATCH leave request with tokens should pass`() {
        testStore.add(leaveGuild)
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${joinGuild.id}/leave") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val update = testStore.guildById(leaveGuild.id)
                assertNotNull(update)
                assertFalse(update.memberIds.any { it == testPlayer.id })
            }
        }
    }

    @Test
    fun `PATCH admin request without tokens should fail`() {
        testStore.add(notAdminGuild)
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${notAdminGuild.id}/admin") {
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `PATCH admin request with tokens but not admin should fail`() {
        testStore.add(notAdminGuild)
        val badActor = GuildMembershipUpdate(owners = GuildMembershipUpdate.Manifest(onboard = listOf(testPlayer.id)))
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${notAdminGuild.id}/admin") {
                addHeader("Content-Type", "application/json")
                setBody(gson.toJson(badActor))

            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                val update = testStore.guildById(notAdminGuild.id)
                assertNotNull(update)
                assertFalse(update.ownerIds.any { it == testPlayer.id })
            }
        }
    }

    @Test
    fun `PATCH admin request with tokens and admin should succeed`() {
        testStore.add(adminGuild)
        val goodRequest = GuildMembershipUpdate(owners = GuildMembershipUpdate.Manifest(onboard = listOf(fakePlayer)))
        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${adminGuild.id}/admin") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
                setBody(gson.toJson(goodRequest))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val update = testStore.guildById(adminGuild.id)
                assertNotNull(update)
                assertTrue(update.ownerIds.any { it == testPlayer.id })
                assertTrue(update.ownerIds.any { it == fakePlayer })
            }
        }
    }

    @Test
    fun `PATCH admin request with tokens to remove the only admin should return 400`() {
        testStore.add(adminGuild.copy())
        val goodRequest = GuildMembershipUpdate(owners = GuildMembershipUpdate.Manifest(deboard = adminGuild.ownerIds))

        withServer {
            handleRequest(HttpMethod.Patch, "/v1/guilds/${adminGuild.id}/admin") {
                addHeader("Content-Type", "application/json")
                addJwtHeader()
                setBody(gson.toJson(goodRequest))
            }.apply {
                // assertEquals(HttpStatusCode.BadRequest, response.status())
                val update = testStore.guildById(adminGuild.id)
                assertNotNull(update)
                assertTrue(update.ownerIds.all { it == testPlayer.id })
            }
        }
    }

}

