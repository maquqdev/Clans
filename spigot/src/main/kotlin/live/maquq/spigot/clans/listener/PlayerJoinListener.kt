package live.maquq.spigot.clans.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.manager.UserManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener(
    private val userManager: UserManager,
    private val scope: CoroutineScope
) : Listener {
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        this.scope.launch {
            userManager.getUser(player.uniqueId) ?: userManager.createNewUser(player)
        }

    }
}