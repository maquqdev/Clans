package live.maquq.spigot.clans.manager

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.maquq.api.DataSource
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserManager(
    private val dataSource: DataSource,
    private val clanManager: ClanManager,
    private val logger: BukkitLogger,
    private val scope: CoroutineScope
) {

    private val userCache: MutableMap<UUID, User> = ConcurrentHashMap()

    suspend fun getUser(uuid: UUID): User? {
        val cachedUser = this.userCache[uuid]
        if (cachedUser != null) {
            this.logger.debug("Loaded $uuid from cache.")
            return cachedUser
        }

        this.logger.debug("Can't find $uuid in cache. Loading from database...")
        val userFromDb = this.dataSource.loadUser(uuid) ?: return null

        userFromDb.init { clanTag ->
            if (clanTag == null) null else this.clanManager.getClan(clanTag)
        }

        this.userCache[uuid] = userFromDb
        this.logger.debug("Saved user $uuid in cache.")

        return userFromDb
    }

    suspend fun saveUser(user: User) {
        this.logger.debug("Saved user ${user.uuid} to database and cache")
        this.dataSource.saveUser(user)
        this.userCache[user.uuid] = user
    }

    suspend fun removeUser(user: User) {
        this.logger.debug("Deleted ${user.uuid} from database and cache.")
        this.dataSource.removeUser(user)
        this.userCache.remove(user.uuid)
    }

    fun handlePlayerQuit(uuid: UUID) {
        scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, exception ->
            logger.error("Failed to save user data for player ${uuid}: ${exception.message}")
            exception.printStackTrace()
        }) {
            try {
                val user = getUser(uuid)
                if (user != null)
                    saveUser(user)
            } catch (exception: Exception) {
                logger.error("Unexpected error saving user data for ${uuid}: ${exception.message}")
            }
        }
        this.logger.debug("Removed $uuid from cache and saved.")
    }

    fun createNewUser(player: Player): User {
        this.logger.debug("Creating a new user for player: ${player.name} (${player.uniqueId})")
        return User(uuid = player.uniqueId)
    }
}