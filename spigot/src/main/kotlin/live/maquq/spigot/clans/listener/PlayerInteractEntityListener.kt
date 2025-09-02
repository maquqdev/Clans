package live.maquq.spigot.clans.listener

import com.bruhdows.minitext.MiniText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

class PlayerInteractEntityListener(
    private val userManager: UserManager,
    private val scope: CoroutineScope,
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration
) : Listener {

    @EventHandler
    fun handlePlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        val target = event.rightClicked
        if(target is Player && player.isSneaking) {
            this.scope.launch {
                val user = userManager.getUser(player.uniqueId)
                val targetUser = userManager.getUser(target.uniqueId)

                userManager.sendInfo(
                    user = user,
                    targetUser = targetUser,
                    mainConfig = mainConfig,
                    miniText = miniText
                )
            }
        }
    }
}