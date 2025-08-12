package live.maquq.spigot.clans.manager

import live.maquq.api.DataSource
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserManager(
    private val dataSource: DataSource,
    private val clanManager: ClanManager,
    private val logger: BukkitLogger
) {

    private val userCache: MutableMap<UUID, User> = ConcurrentHashMap()

    suspend fun getUser(uuid: UUID): User? {
        val cachedUser = this.userCache[uuid]
        if (cachedUser != null) {
            this.logger.debug("Pobrano użytkownika $uuid z cache'u.")
            return cachedUser
        }

        this.logger.debug("Brak użytkownika $uuid w cache'u. Ładowanie z bazy danych...")
        val userFromDb = this.dataSource.loadUser(uuid) ?: return null

        userFromDb.init { clanTag ->
            if (clanTag == null) null else this.clanManager.getClan(clanTag)
        }

        this.userCache[uuid] = userFromDb
        this.logger.debug("Zapisano użytkownika $uuid w cache'u.")

        return userFromDb
    }

    suspend fun saveUser(user: User) {
        this.logger.debug("Zapisywanie użytkownika ${user.uuid} do bazy danych i cache'u.")
        this.dataSource.saveUser(user)
        this.userCache[user.uuid] = user
    }

    suspend fun removeUser(user: User) {
        this.logger.info("Usuwanie użytkownika ${user.uuid} z bazy danych i cache'u.")
        this.dataSource.removeUser(user)
        this.userCache.remove(user.uuid)
    }

    fun handlePlayerQuit(uuid: UUID) {
        this.userCache.remove(uuid)
        this.logger.debug("Usunięto użytkownika $uuid z cache'u po wyjściu z serwera.")
    }

    fun createNewUser(player: Player): User {
        this.logger.info("Tworzenie nowego profilu dla gracza: ${player.name} (${player.uniqueId})")
        return User(uuid = player.uniqueId)
    }
}