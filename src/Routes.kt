package dog.wildtulsa


import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.locations.delete
import io.ktor.locations.get
import io.ktor.locations.patch
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import java.util.*


fun Route.players(store: DAOFacade, hash: (String) -> String) {
    authenticate {
        get<Players> {
            val players = store.listAllPlayers()
            call.respond(mapOf("players" to players))
        }

        get<Profile> { profile ->
            val id = UUID.fromString(profile.id)
            val player = store.playerProfile(id)
            if (player != null) {
                call.respond(player)
            } else {
                call.respond(HttpStatusCode.NotFound, "Player not found")
            }
        }
    }

    // intentionally leave this open to allow for first time user creation.
    post<Players> {
        val json = call.receive<PostPlayer>()
        val player = Player(name = json.player.name, password = hash(json.player.password))
        store.add(player)
        call.respond(player)
    }
}


fun Route.guilds(store: DAOFacade) {
    authenticate {

        // *A player should be able to search for existing guilds by name
        get<Guilds> { query ->
            val q = query.q
            val guilds = store.listAllGuilds().filter { it.name.startsWith(q, true) }
            call.respond(mapOf("guilds" to guilds))
        }

        // *A player should be able to create a guild (becoming the guild owner)
        post<Guilds> {
            val json = call.receive<PostGuild>().guild
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val ownerId = UUID.fromString(principal.name)
            val guild = Guild(
                ownerIds = listOf(ownerId),
                name = json.name,
                description = json.description,
                memberIds = json.memberIds
            )
            store.add(guild)
            call.respond(guild)
        }

        // A guild should provide a way of listing all members
        get<GuildProfile> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = store.guildProfile(id)
            if (guild != null) {
                call.respond(guild)
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

        // *A guild owner can disband the guild
        delete<GuildProfile> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = store.guildById(id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val ownerId = UUID.fromString(principal.name)
            if (guild != null && guild.ownerIds.any { it == ownerId }) {
                store.remove("guild", id)
                call.respond(HttpStatusCode.OK, "OK!")
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

        // *A player should be able to leave a guild
        patch<GuildJoin> { profile ->
            val id = UUID.fromString(profile.id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val update = store.addGuildMember(UUID.fromString(principal.name), id)
            if (update !== false) {
                call.respond(HttpStatusCode.OK, "Welcome.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

        // *A player should be able to leave a guild
        patch<GuildLeave> { profile ->
            val id = UUID.fromString(profile.id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val update = store.removePlayerFromGuild(UUID.fromString(principal.name), id)
            if (update !== false) {
                call.respond(HttpStatusCode.OK, "Goodbye.")
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

        // A guild owner can kick people out of the guild
        // *A guild owner can make other members into guild owners
        patch<GuildAdmin> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = store.guildById(id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val ownerId = UUID.fromString(principal.name)
            // this should all be moved somewhere else..
            if (guild != null && guild.ownerIds.any { it == ownerId }) {
                val json = call.receive<GuildMembershipUpdate>()
                val members = guild.memberIds.toMutableList()
                if (json.members !== null) {
                    members.addAll(json.members?.onboard ?: listOf())
                    members.removeAll(json.members?.deboard ?: listOf())
                }

                val owners = guild.ownerIds.toMutableList()
                if (json.owners !== null) {
                    owners.addAll(json.owners?.onboard ?: listOf())
                    owners.removeAll(json.owners?.deboard ?: listOf())
                }

                val update = guild.copy(ownerIds = owners.distinct(), memberIds = members.distinct())
                store.add(update)
                call.respond(HttpStatusCode.OK, "OK")
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }


    }
}

/*

fun Route.invites(inviteStore: InviteStore) {
    authenticate {
        get<Invites> {
            val invites = inviteStore.top()
            call.respond(mapOf("invites" to invites))
        }

        post<Invites> {
            val json = call.receive<PostInvite>().invite
            val invite = Invite(invitorId = json.invitorId, inviteeId = json.inviteeId, guildId = json.guildId)
            inviteStore.add(invite)
            call.respond(invite)
        }
    }
}
*/
