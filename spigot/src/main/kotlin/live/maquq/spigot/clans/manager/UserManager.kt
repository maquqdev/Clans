package live.maquq.spigot.clans.manager

import live.maquq.api.DataSource
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger
import org.bukkit.entity.Player
import java.util.*

class UserManager(
    private val dataSource: DataSource,
    private val clanManager: ClanManager,
    private val logger: BukkitLogger
) {

    suspend fun getUser(uuid: UUID): User? {
        this.logger.debug("Pobieranie użytkownika o UUID: $uuid")
        val user = this.dataSource.loadUser(uuid) ?: return null

        user.init { clanTag ->
            if (clanTag == null) {
                null
            } else {
                this.clanManager.getClan(clanTag)
            }
        }

        return user
    }

    suspend fun saveUser(user: User) {
        this.logger.debug("Zapisywanie użytkownika: ${user.uuid}")
        this.dataSource.saveUser(user)
    }

    fun createNewUser(player: Player): User {
        this.logger.info("Tworzenie nowego profilu dla gracza: ${player.name} (${player.uniqueId})")
        return User(
            uuid = player.uniqueId
        )
    }
}