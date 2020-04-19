package dog.wildtulsa

import io.ktor.auth.*
import java.util.*
import java.io.*

interface Identifiable {
    val id: UUID
}

// Used when posting a player
data class PostPlayer(val player: PostPlayer.Cred ) {
    data class Cred(val name: String, val password: String)
}

// to do: break "user" functionality from "player"
data class Player(
    override val id: UUID = UUID.randomUUID(),
    val name: String,
    val password: String
): Identifiable, Principal, Serializable

data class PostGuild( val guild: Guild )

data class Guild(
    override val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String,
    val ownerIds: List<UUID>,
    var memberIds: List<UUID> = listOf()
): Identifiable, Serializable

fun Guild.validate(): Boolean = ownerIds.isNotEmpty()

data class GuildMembershipUpdate( val owners: GuildMembershipUpdate.Manifest? , val members: GuildMembershipUpdate.Manifest?) {
    data class Manifest(val onboard: List<UUID>?, val deboard: List<UUID>?)
}

data class PostInvite(val invite: Invite)

data class Invite(
    override val id: UUID = UUID.randomUUID(),
    val invitorId: UUID,
    val inviteeId: UUID,
    val guildId: UUID
): Identifiable, Serializable
