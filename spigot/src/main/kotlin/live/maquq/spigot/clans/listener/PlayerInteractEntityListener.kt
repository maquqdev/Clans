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
import org.bukkit.inventory.EquipmentSlot

class PlayerInteractEntityListener(
    private val userManager: UserManager,
    private val scope: CoroutineScope,
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration
) : Listener {

    @EventHandler
    fun handlePlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand == EquipmentSlot.OFF_HAND) return
        val player = event.player

        val target = event.rightClicked
        if(target is Player && player.isSneaking) {
            this.scope.launch {
                val targetUser = userManager.getUser(target.uniqueId)

                userManager.sendInfo(
                    player = player,
                    targetUser = targetUser,
                    mainConfig = mainConfig,
                    miniText = miniText
                )
            }
        }
    }
}