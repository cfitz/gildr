package dog.wildtulsa

import io.ktor.auth.*
import java.util.*
import java.io.*

interface Identifiable {
    val id: UUID
    val type: String
}

// Used when posting a player
data class PostPlayer(val player: PostPlayer.Cred) {
    data class Cred(val name: String, val password: String)
}

// to do: break "user" functionality from "player"
data class Player(
    override val type: String = "player",
    override val id: UUID = UUID.randomUUID(),
    val memberOf: List<Guild> = listOf(),
    val ownerOf: List<Guild> = listOf(),
    val name: String,
    val password: String
) : Identifiable, Principal, Serializable {
    init {
        require( name.isNotBlank() ) { "All players must have a name."}
        require( password.isNotBlank() ) { "All players must have a password."}
    }
}

data class PostGuild(val guild: PostGuild.Registration) {
    data class Registration( val name: String, val description: String, val memberIds: List<UUID> = listOf())
}

data class Guild(
    override val id: UUID = UUID.randomUUID(),
    override val type: String = "guild",
    val name: String,
    val description: String,
    val owners: List<Player> = listOf(),
    val ownerIds: List<UUID>,
    val members: List<Player> = listOf(),
    var memberIds: List<UUID> = listOf()
) : Identifiable, Serializable {
    init {
        require( ownerIds.isNotEmpty()) { "Guild ${name} must have at least one owner" }
        require( name.isNotBlank() ) { "All guilds must have a name."}
        require( description.isNotBlank() ) { "All guilds must have a description."}
    }
}

data class GuildMembershipUpdate(
    val owners: GuildMembershipUpdate.Manifest = GuildMembershipUpdate.Manifest(),
    val members: GuildMembershipUpdate.Manifest = GuildMembershipUpdate.Manifest()
) {
    data class Manifest(val onboard: List<UUID> = listOf(), val deboard: List<UUID> = listOf())
}

data class PostInvite(val invite: Invite)

data class Invite(
    override val id: UUID = UUID.randomUUID(),
    override val type: String = "invite",
    val invitorId: UUID,
    val inviteeId: UUID,
    val guildId: UUID
) : Identifiable, Serializable
