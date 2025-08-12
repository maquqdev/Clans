package live.maquq.storage.impl

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.maquq.api.DataSource
import live.maquq.api.clan.Clan
import live.maquq.api.User
import java.util.*

class MongoDataSource(private val connectionString: String) : DataSource {

    private lateinit var client: MongoClient
    private lateinit var usersCollection: MongoCollection<User>
    private lateinit var clansCollection: MongoCollection<Clan>

    override fun connect() {
        client = MongoClients.create(connectionString)
        val database = client.getDatabase("clans")

        usersCollection = database.getCollection("users", User::class.java)
        clansCollection = database.getCollection("clans", Clan::class.java)
    }

    override fun disconnect() {
        if (this::client.isInitialized) {
            client.close()
        }
    }

    override suspend fun loadUser(uuid: UUID): User? = withContext(Dispatchers.IO) {
        usersCollection.find(Filters.eq("_id", uuid)).first()
    }

    override suspend fun saveUser(user: User) {
        withContext(Dispatchers.IO) {
            usersCollection.replaceOne(Filters.eq("_id", user.uuid), user, ReplaceOptions().upsert(true))
        }
    }

    override suspend fun loadClan(tag: String): Clan? = withContext(Dispatchers.IO) {
        clansCollection.find(Filters.eq("_id", tag)).first()
    }

    override suspend fun saveClan(clan: Clan) {
        withContext(Dispatchers.IO) {
            clansCollection.replaceOne(Filters.eq("_id", clan.tag), clan, ReplaceOptions().upsert(true))
        }
    }

    override suspend fun deleteClan(tag: String) {
        withContext(Dispatchers.IO) {
            clansCollection.deleteOne(Filters.eq("_id", tag))
            usersCollection.updateMany(
                Filters.eq("clanTag", tag),
                Updates.unset("clanTag")
            )
        }
    }

    override suspend fun getAllClans(): List<Clan> = withContext(Dispatchers.IO) {
        clansCollection.find().toList()
    }
}