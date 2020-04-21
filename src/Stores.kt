package dog.wildtulsa

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import org.ehcache.CacheManagerBuilder
import org.ehcache.config.CacheConfigurationBuilder
import org.ehcache.config.ResourcePoolsBuilder
import org.ehcache.config.persistence.CacheManagerPersistenceConfiguration
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import java.io.File
import java.io.IOException
import java.util.*

val File.nameWithoutAllExtensions: String
    get() = name.substringBefore(".")

// https://kotlinlang.org/docs/reference/delegation.html
interface DAOFacade {

    fun init()

    val gson: Gson
        get() = GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create()

    fun listAllPlayers(): Sequence<Player>
    fun listAllGuilds(): Sequence<Guild>

    fun byId(type: String, id: UUID): Identifiable?
    fun playerById(id: UUID): Player?
    fun guildById(id: UUID): Guild?

    fun guildProfile(id: UUID): Guild?
    fun playerProfile(id: UUID): Player?

    fun add(record: Identifiable): UUID
    fun remove(type: String, id: UUID): Boolean

    fun removePlayerFromGuild(playerId: UUID, guildId: UUID): Boolean
    fun addGuildMember(playerId: UUID, guildId: UUID): Boolean


}

class DAOFacadeStore(val dataDir: File) : DAOFacade {

    private fun getFile(type: String, id: UUID): File {
        return File(dataDir, "$id.$type.idx")
    }

    private fun makeDataDir(dirPath: String) {
        val dir = File(dirPath)
        if (!dir.mkdirs() && !dir.exists()) {
            throw IOException("Failed to create directory ${dir.absolutePath}")
        }
    }

    override fun init() {
        val dataDirPath = dataDir.toString()
        makeDataDir(dataDirPath)
    }

    private val allGuildIds by lazy {
        dataDir.listFiles { f -> f.nameWithoutExtension.endsWith("guild") }
            .mapTo(ArrayList()) { UUID.fromString(it.nameWithoutAllExtensions) }
            .toMutableSet()
    }

    private val allPlayerIds by lazy {
        dataDir.listFiles { f -> f.nameWithoutExtension.endsWith("player") }
            .mapTo(ArrayList()) { UUID.fromString(it.nameWithoutAllExtensions) }
            .toMutableSet()
    }

    override fun byId(type: String, id: UUID): Identifiable? {
        try {
            val file = getFile(type, id)
            if (type == "guild") {
                return gson.fromJson(file.readText(), Guild::class.java)
            } else {
                return gson.fromJson(file.readText(), Player::class.java)
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override fun add(record: Identifiable): UUID {
        val type = record.type
        val file = getFile(type, record.id)
        file.writeText(gson.toJson(record))
        if (type == "guild") {
            allGuildIds.add(record.id)
        } else {
            allPlayerIds.add(record.id)
        }

        return record.id
    }

    override fun remove(type: String, id: UUID): Boolean {
        val file = getFile(type, id)
        if (file.exists()) {
            file.delete()
        }
        if (type == "guild") {
            allGuildIds.remove(id)
        } else {
            allPlayerIds.remove(id)
        }
        return true
    }


    override fun listAllPlayers(): Sequence<Player> =
        allPlayerIds.asSequence().mapNotNull { byId("player", it) as Player }

    override fun listAllGuilds(): Sequence<Guild> = allGuildIds.asSequence().mapNotNull { byId("guild", it) as Guild } ?: sequenceOf()

    override fun playerById(id: UUID): Player? {
        val player = byId("player", id) ?: return null
        return player as Player
    }

    override fun guildById(id: UUID): Guild? {
        val guild = byId("guild", id) ?: return null
        return guild as Guild
    }

    override fun playerProfile(id: UUID): Player? {
        val player = playerById(id) ?: return null
        val memberOf = guildsByMemberId(player.id)
        val ownerOf = guildsByOwnerId(player.id)
        return player.copy(memberOf = memberOf, ownerOf = ownerOf)
    }

    override fun guildProfile(id: UUID): Guild? {
        val guild = guildById(id) ?: return null
        val members = guild.memberIds.map { playerById(it) }.mapNotNull { it }
        val owners = guild.ownerIds.map { playerById(it) }.mapNotNull { it }
        // double yuck.
        return guild.copy(members = members, owners = owners)
    }

    // Yuck
    private fun guildsByMemberId(id: UUID): List<Guild> {
        return listAllGuilds().filter { it.memberIds.any { it2 -> it2 == id } }.toList()
    }

    // Yuck
    private fun guildsByOwnerId(id: UUID): List<Guild> {
        return listAllGuilds().filter { it.ownerIds.any { it2 -> it2 == id } }.toList()
    }

    override fun removePlayerFromGuild(playerId: UUID, guildId: UUID): Boolean {
        val player = playerById(playerId) ?: return false
        val guild = guildById(guildId) ?: return false

        val memberIds = guild.memberIds.filterNot { it == player.id }.distinct()
        val ownerIds = guild.ownerIds.filterNot { it == player.id }.distinct()
        val update = guild.copy(memberIds = memberIds, ownerIds = ownerIds)
        add(update)
        return true
    }

    override fun addGuildMember(playerId: UUID, guildId: UUID): Boolean {
        val player = playerById(playerId) ?: return false
        val guild = guildById(guildId) ?: return false
        val memberIds = guild.memberIds.union(listOf(player.id)).toList()
        val update = guild.copy(memberIds = memberIds)
        add(update)
        return true
    }


}

class DAOFacadeCache(val delegate: DAOFacade, val cacheDir: File) : DAOFacade {

    val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(cacheDir))
        .withCache(
            "guildCache",
            CacheConfigurationBuilder.newCacheConfigurationBuilder<UUID, Guild>()
                .withResourcePools(
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(1000, EntryUnit.ENTRIES)
                        .offheap(10, MemoryUnit.MB)
                        .disk(100, MemoryUnit.MB, true)
                ).buildConfig(UUID::class.javaObjectType, Guild::class.java)
        )
        .withCache(
            "playerCache",
            CacheConfigurationBuilder.newCacheConfigurationBuilder<UUID, Player>()
                .withResourcePools(
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(1000, EntryUnit.ENTRIES)
                        .offheap(10, MemoryUnit.MB)
                        .disk(100, MemoryUnit.MB, true)
                ).buildConfig(UUID::class.javaObjectType, Player::class.java)
        )
        .build(true)

    private val guildCache = cacheManager.getCache("guildCache", UUID::class.javaObjectType, Guild::class.java)
    private val playerCache = cacheManager.getCache("playerCache", UUID::class.javaObjectType, Player::class.java)

    override fun init() {
        delegate.init()
    }

    override fun listAllPlayers() = delegate.listAllPlayers()
    override fun listAllGuilds() = delegate.listAllGuilds()

    override fun byId(type: String, id: UUID): Identifiable? {
        if (type == "guild") {
            return guildById(id)
        } else {
            return playerById(id)
        }
    }

    override fun playerById(id: UUID): Player? {
        val record = playerCache.get(id)
        if (record != null) {
            return record
        }
        return try {
            val player = delegate.playerById(id)
            playerCache.put(id, player)
            player
        } catch (e: Throwable) {
            null
        }

    }

    override fun guildById(id: UUID): Guild? {
        val record = guildCache.get(id)
        if (record != null) {
            return record
        }
        try {
            val guild = delegate.guildById(id)
            guildCache.put(id, guild)
            return guild
        } catch (e: Throwable) {
            return null
        }
    }

    override fun guildProfile(id: UUID): Guild? = delegate.guildProfile(id)
    override fun playerProfile(id: UUID): Player? = delegate.playerProfile(id)

    override fun add(record: Identifiable): UUID {
        val id = record.id
        val type = record.type
        delegate.add(record)
        if (type == "guild") {
            guildCache.put(id, record as Guild)
        } else {
            playerCache.put(id, record as Player)
        }
        return id
    }

    override fun remove(type: String, id: UUID): Boolean {
        delegate.remove(type, id)
        if (type == "guild") {
            guildCache.remove(id)
        } else {
            playerCache.remove(id)
        }
        return true
    }

    override fun removePlayerFromGuild(playerId: UUID, guildId: UUID): Boolean {
        val update = delegate.removePlayerFromGuild(playerId, guildId)
        if (update == true) {
            guildCache.remove(guildId)
        }
        return update
    }

    override fun addGuildMember(playerId: UUID, guildId: UUID): Boolean {
        val update = delegate.addGuildMember(playerId, guildId)
        if (update == true) {
            guildCache.remove(guildId)
        }
        return update
    }

}

