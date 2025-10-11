package live.maquq.spigot.clans.listener

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.manager.UserManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.log

class PlayerQuitListener(
    private val userManager: UserManager,
) : Listener {

    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        this.userManager.handlePlayerQuit(event.player.uniqueId)
    }
}