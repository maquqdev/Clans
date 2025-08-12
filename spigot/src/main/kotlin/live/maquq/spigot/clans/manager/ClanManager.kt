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
            this.logger.debug("Pobrano klan '$tag' z cache'u.")
            return cachedClan
        }

        this.logger.debug("Brak klanu '$tag' w cache'u. Ładowanie z bazy danych...")
        val clanFromDb = this.dataSource.loadClan(tag) ?: return null

        clanFromDb.init { ownerUuid ->
            if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid)
        }

        this.clanCache[tag] = clanFromDb
        this.logger.debug("Zapisano klan '$tag' w cache'u.")

        return clanFromDb
    }

    suspend fun saveClan(clan: Clan) {
        this.logger.debug("Zapisywanie klanu ${clan.tag} do bazy danych i cache'u.")
        this.dataSource.saveClan(clan)
        this.clanCache[clan.tag] = clan
    }

    suspend fun deleteClan(clan: Clan) {
        this.logger.warn("Usuwanie klanu ${clan.tag} z bazy danych i cache'u.")

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
        this.logger.info("Wczytywanie wszystkich klanów do cache'u...")
        val allClans = this.dataSource.getAllClans()
        allClans.forEach { clan ->
            clan.init { ownerUuid -> if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid) }
            this.clanCache[clan.tag] = clan
        }
        this.logger.info("Załadowano ${allClans.size} klanów do cache'u.")
    }

    fun createNewClan(tag: String, owner: User): Clan {
        this.logger.info("Tworzenie nowego klanu '$tag' przez gracza ${owner.uuid}")
        return Clan(
            tag = tag,
            ownerUuid = owner.uuid,
            members = mutableMapOf(owner.uuid to ClanRole.LEADER)
        )
    }
}