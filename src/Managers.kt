package dog.wildtulsa

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import java.io.File
import java.util.*

interface ManagerInterface<T: Identifiable> {
    val gson: Gson
        get() =  GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create()

    val modelKlass: Class<T>
    val dataDir: File

    fun buildFile(id: UUID): File {
        return File(dataDir, "$id.idx")
    }

    fun fromFile(id: UUID): T {
        return gson.fromJson(buildFile(id).readText(), modelKlass)
    }

    fun toFile(record: T): Unit {
        return buildFile(record.id).writeText(gson.toJson(record))
    }

    fun deleteFile(id: UUID): Boolean {
       val file = buildFile(id)
        if (file.exists()) {
            return file.delete()
        }
        return true
    }
}

class PlayerManager(override val dataDir: File) : ManagerInterface<Player> {
    override val modelKlass = Player::class.java
}

class GuildManager(override val dataDir: File): ManagerInterface<Guild> {
    override val modelKlass = Guild::class.java
}

class InviteManager(override val dataDir: File): ManagerInterface<Invite> {
    override val modelKlass = Invite::class.java
}

