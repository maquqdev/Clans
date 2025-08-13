package live.maquq.spigot.clans.manager


import live.maquq.api.DataSource
import live.maquq.api.clan.Clan
import live.maquq.api.clan.ClanRole
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger
import java.util.concurrent.ConcurrentHashMap

class ClanManager(
    private val dataSource: DataSource,
    private val logger: BukkitLogger
) {

    private val clanCache: MutableMap<String, Clan> = ConcurrentHashMap()

    suspend fun getClan(tag: String): Clan? {
        val cachedClan = this.clanCache[tag]
        if (cachedClan != null) {
            this.logger.debug("Loaded '$tag' from cache.")
            return cachedClan
        }

        this.logger.debug("Cannot find '$tag' in cache, loading from database...")
        val clanFromDb = this.dataSource.loadClan(tag) ?: return null

        clanFromDb.init { ownerUuid ->
            if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid)
        }

        this.clanCache[tag] = clanFromDb
        this.logger.debug("Saved clan '$tag' in cache.")

        return clanFromDb
    }

    suspend fun saveClan(clan: Clan) {
        this.logger.debug("Saving ${clan.tag} to database and cache...")
        this.dataSource.saveClan(clan)
        this.clanCache[clan.tag] = clan
    }

    suspend fun deleteClan(clan: Clan) {
        this.logger.debug("Deleting clan ${clan.tag} from database and cache...")

        for (memberUuid in clan.members.keys) {
            val user = dataSource.loadUser(memberUuid)
            if (user != null) {
                val updatedUser = user.copy(clanTag = null)
                dataSource.saveUser(updatedUser)
            }
        }

        this.dataSource.deleteClan(clan.tag)
        this.clanCache.remove(clan.tag)
    }

    suspend fun preloadAllClansToCache() {
        this.logger.debug("Loading every clan to cache...")
        val allClans = this.dataSource.getAllClans()
        allClans.forEach { clan ->
            clan.init { ownerUuid -> if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid) }
            this.clanCache[clan.tag] = clan
        }
        this.logger.debug("Loaded ${allClans.size} clans to cache.")
    }

    fun createNewClan(tag: String, owner: User): Clan {
        this.logger.debug("Creating clan '$tag' to owner ${owner.uuid}")
        return Clan(
            tag = tag,
            ownerUuid = owner.uuid,
            members = mutableMapOf(owner.uuid to ClanRole.LEADER)
        )
    }
}