package live.maquq.spigot.clans.manager

import com.bruhdows.minitext.MiniText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.maquq.api.data.DataSource
import live.maquq.api.user.User
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserManager(
    private val dataSource: DataSource,
    private val logger: BukkitLogger,
    private val scope: CoroutineScope,
    private val mainConfig: PluginConfiguration
) {

    private val userCache: MutableMap<UUID, User> = ConcurrentHashMap()

    suspend fun getUser(uuid: UUID): User {
        this.userCache[uuid].let {
            if (it != null) {
                this.logger.debug("Loaded $uuid from cache.")
                return it
            }
        }

        this.logger.debug("Can't find $uuid in cache. Loading from database...")
        val userFromDb = this.dataSource.loadUser(uuid) ?: createNewUser(uuid, mainConfig.clanSettings.defaultPoints)

        this.userCache[uuid] = userFromDb
        this.logger.debug("Saved user $uuid in cache.")

        return userFromDb
    }

    suspend fun saveUser(user: User) {
        this.logger.debug("Saved user ${user.uuid} to database and cache")
        this.dataSource.saveUser(user)
        this.userCache[user.uuid] = user
    }

    fun handlePlayerQuit(uuid: UUID) {
        this.scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, exception ->
            this.logger.error("Failed to save user data for player ${uuid}: ${exception.message}")
            exception.printStackTrace()
        }) {
            try {
                saveUser(getUser(uuid))
            } catch (exception: Exception) {
                logger.error("Unexpected error saving user data for ${uuid}: ${exception.message}")
            }
        }
        this.logger.debug("Saved $uuid.")
    }

    fun createNewUser(uuid: UUID, defaultPoints: Int): User {
        this.logger.debug("Creating a new user for player: $uuid")
        return User(
            uuid = uuid,
            points = defaultPoints
        )
    }

    fun sendInfo(
        player: Player,
        targetUser: User,
        mainConfig: PluginConfiguration,
        miniText: MiniText
    ) {
        val clanTag = targetUser.clanTag ?: "BRAK"
        val kdFormatted = if (targetUser.deaths > 0)
            String.format("%.2f", targetUser.kills.toDouble() / targetUser.deaths)
        else
            targetUser.kills.toString()

        val message = mainConfig.messages.playerInfo
            .replace("[PLAYER]", Bukkit.getPlayer(targetUser.uuid)!!.name)
            .replace("[TAG]", clanTag)
            .replace("[POINTS]", targetUser.points.toString())
            .replace("[DEATHS]", targetUser.deaths.toString())
            .replace("[KILLS]", targetUser.kills.toString())
            .replace("[KD]", kdFormatted)

        miniText.deserialize(message).component().let {
            player.sendMessage(it)
        }
    }

    fun all(): List<User> {
        return this.userCache.values.toList()
    }
}