package dog.wildtulsa

import io.ktor.application.Application
import org.ehcache.CacheManagerBuilder
import org.ehcache.config.CacheConfigurationBuilder
import java.io.File
import java.util.*

open class Store<T:Identifiable>(val fileManager: ManagerInterface<T>) {

    val cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true)
    val cache = cacheManager.createCache<UUID, T>(fileManager.modelKlass.simpleName,
        CacheConfigurationBuilder.newCacheConfigurationBuilder<UUID, T>().buildConfig(Class.forName("java.util.UUID") as Class<UUID>,
            fileManager.modelKlass
        ))

    fun listAll(): Sequence<T> = allIds.asSequence().mapNotNull { byId(it) }
    fun top() = listAll().take(10).toList()

    val allIds by lazy {
        fileManager.dataDir.listFiles { f -> f.extension == "idx" }.mapTo(ArrayList()) { UUID.fromString(it.nameWithoutExtension) }
    }

    fun byId(id: UUID): T? {
        val record = cache.get(id)
        if (record != null) {
            return record
        }

        try {
            val json = fileManager.fromFile(id)
            cache.put(id, json)

            return json
        } catch (e: Throwable) {
            return null
        }
    }


    fun add(record: T): UUID {
        val id = record.id
        fileManager.toFile(record)
        allIds.add(id)
        cache.put(id, record)
        return id
    }

    fun remove(id: UUID): Boolean {
        val record = byId(id)
        if (record != null) {
            cache.remove(id)
            fileManager.deleteFile(id)
        }
        return true
    }


}

class PlayerStore(val dataDir: File) : Store<Player>(fileManager = PlayerManager(dataDir = dataDir) ) {
}


class GuildStore(val dataDir: File) : Store<Guild>(fileManager = GuildManager(dataDir = dataDir) ) {

   fun byMember(id: UUID): List<Guild> {
       return listAll().filter { it.memberIds.any { id -> id == id } }.toList()
   }

    fun byOwner(id: UUID): List<Guild> {
        return listAll().filter { it.ownerIds.any { id -> id == id } }.toList()
    }

}

class InviteStore(val dataDir: File) : Store<Invite>(fileManager = InviteManager(dataDir = dataDir) ) {

    fun byInvitor(id: UUID): List<Invite> {
        return listAll().filter { it.invitorId == id }.toList()
    }

    fun byInvitee(id: UUID): List<Invite> {
        return listAll().filter { it.inviteeId == id }.toList()
    }
}
