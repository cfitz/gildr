package dog.wildtulsa


import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.getDigestFunction
import io.ktor.util.hex
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun hash(password: String): String {
    val digester = getDigestFunction("SHA-256") { "ktor${it.length}" }
    val digest = digester(password)
    return Base64.getEncoder().encodeToString(digest)
}

fun Route.players(playerStore: PlayerStore) {
    authenticate {
        get<Players> {
            val players = playerStore.listAll()
            call.respond(mapOf("players" to players))
        }
    }

    // intentionally leave this open to allow for first time user creation.
    post<Players> {
        val json = call.receive<PostPlayer>()
        val player = Player(name = json.player.name, password = hash(json.player.password))
        playerStore.add(player)
        call.respond(player)
    }
}

/* This is bad. Instead there should be a profileStore that has the information. */
fun Route.playerProfile(playerStore: PlayerStore, guildStore: GuildStore, inviteStore: InviteStore) {
    authenticate {
        get<Profile> { profile ->
            val id = UUID.fromString(profile.id)
            val player = playerStore.byId(id)
            if (player != null) {
                call.respond(
                    mapOf(
                        "player" to player,
                        "memberOf" to guildStore.byMember(id),
                        "ownerOf" to guildStore.byOwner(id),
                        "invitesSent" to inviteStore.byInvitor(id),
                        "invitesReceived" to inviteStore.byInvitee(id)
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, "Player not found")
            }
        }
    }
}


fun Route.guilds(guildStore: GuildStore) {
    authenticate {

        get<Guilds> { query ->
            val q = query.q
            val guilds = guildStore.listAll().filter { it.name.startsWith(q, true) }
            call.respond(mapOf("guilds" to guilds))
        }

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
            guildStore.add(guild)
            call.respond(guild)
        }
    }
}

/* This is bad. Instead there should be a store that has the information. */
fun Route.guildProfile(playerStore: PlayerStore, guildStore: GuildStore, inviteStore: InviteStore) {
    authenticate {
        get<GuildProfile> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = guildStore.byId(id)
            if (guild != null) {
                call.respond(
                    mapOf(
                        "guild" to guild,
                        "members" to guild.memberIds.map { playerStore.byId(it) },
                        "owners" to guild.ownerIds.map { playerStore.byId(it) },
                        "invitesSent" to inviteStore.byInvitor(id),
                        "invitesReceived" to inviteStore.byInvitee(id)
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

        delete<GuildProfile> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = guildStore.byId(id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val ownerId = UUID.fromString(principal.name)
            if (guild != null && guild.ownerIds.any { it == ownerId }) {
                guildStore.remove(id)
                call.respond(HttpStatusCode.OK, "OK!")
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }


    }
}

fun Route.guildMembership(guildStore: GuildStore) {
    authenticate {

        put<GuildJoin> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = guildStore.byId(id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val player = UUID.fromString(principal.name)
            if (guild != null) {
                val memberIds = guild.memberIds.union(listOf(UUID.fromString(principal.name))).distinct()
                val update = guild.copy(memberIds = memberIds)
                if (update.validate()) {
                    guildStore.add(update)
                    call.respond(update)
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

        put<GuildLeave> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = guildStore.byId(id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val player = UUID.fromString(principal.name)
            if (guild != null) {
                val uuid = UUID.fromString(principal.name)
                val memberIds = guild.memberIds.filterNot { it == uuid  }.distinct()
                val ownerIds = guild.ownerIds.filterNot { it == uuid }.distinct()
                val update = guild.copy(memberIds = memberIds, ownerIds = ownerIds)
                if (update.validate()) {
                    guildStore.add(update)
                    call.respond(update)
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }


        put<GuildMembership> { profile ->
            val id = UUID.fromString(profile.id)
            val guild = guildStore.byId(id)
            val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
            val ownerId = UUID.fromString(principal.name)
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
                if (update.validate()) {
                    guildStore.add(update)
                    call.respond(update)
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "Guild not found")
            }
        }

    }
}

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
