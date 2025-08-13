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
    private val scope: CoroutineScope,
//    private val logger: BukkitLogger
) : Listener {

    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        userManager.handlePlayerQuit(event.player.uniqueId) //im confused..!>!>!>!>
//        this.scope.launch {
//        }
//        val player = event.player ?: return
//        val playerUuid = player.uniqueId ?: return
//
//        scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, exception ->
//            logger.error("Failed to save user data for player ${player.name} (${playerUuid}): ${exception.message}")
//            exception.printStackTrace()
//        }) {
//            try {
//                val user = userManager.getUser(playerUuid)
//                if (user != null)
//                    userManager.saveUser(user)
//            } catch (exception: Exception) {
//                logger.error("Unexpected error saving user data for ${player.name}: ${exception.message}")
//            }
//        }
    }
}