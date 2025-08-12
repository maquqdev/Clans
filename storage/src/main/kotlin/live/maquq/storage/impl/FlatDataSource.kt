package live.maquq.storage.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.maquq.api.DataSource
import live.maquq.api.User
import live.maquq.api.clan.Clan
import java.io.File
import java.util.UUID


class FlatDataSource(private val dataFolder: File) : DataSource {

    private val userFolder = File(dataFolder, "users")
    private val clanFolder = File(dataFolder, "clans")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun connect() {
        if (!userFolder.exists()) userFolder.mkdirs()
        if (!clanFolder.exists()) clanFolder.mkdirs()
    }

    override fun disconnect() { }

    override suspend fun loadUser(uuid: UUID): User? = withContext(Dispatchers.IO) {
        val userFile = File(userFolder, "$uuid.json")
        if (!userFile.exists()) return@withContext null
        runCatching { userFile.reader().use { gson.fromJson(it, User::class.java) } }.getOrNull()
    }

    override suspend fun saveUser(user: User) {
        withContext(Dispatchers.IO) {
            val userFile = File(userFolder, "${user.uuid}.json")
            runCatching { userFile.writer().use { gson.toJson(user, it) } }
                .onFailure { it.printStackTrace() }
        }
    }

    override suspend fun loadClan(tag: String): Clan? = withContext(Dispatchers.IO) {
        val clanFile = File(clanFolder, "$tag.json")
        if (!clanFile.exists()) return@withContext null
        runCatching { clanFile.reader().use { gson.fromJson(it, Clan::class.java) } }.getOrNull()
    }

    override suspend fun saveClan(clan: Clan) {
        withContext(Dispatchers.IO) {
            val clanFile = File(clanFolder, "${clan.tag}.json")
            runCatching { clanFile.writer().use { gson.toJson(clan, it) } }
                .onFailure { it.printStackTrace() }
        }
    }

    override suspend fun deleteClan(tag: String) {
        withContext(Dispatchers.IO) {
            File(clanFolder, "$tag.json").delete()

            userFolder.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
                val user = runCatching { file.reader().use { gson.fromJson(it, User::class.java) } }.getOrNull()
                if (user?.clanTag == tag) {
                    val updatedUser = user.copy(clanTag = null)

                    saveUser(updatedUser)
                }
            }
        }
    }

    override suspend fun getAllClans(): List<Clan> = withContext(Dispatchers.IO) {
        clanFolder.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { file ->
                runCatching { file.reader().use { gson.fromJson(it, Clan::class.java) } }.getOrNull()
            } ?: emptyList()
    }
}