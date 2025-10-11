package live.maquq.spigot.gui.listener

import live.maquq.spigot.gui.internal.InventoryImpl
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent

internal class InventoryListener(private val manager: InventoryManager) : Listener {

    @EventHandler
    fun handleInventoryClick(event: InventoryClickEvent) {
        val inventory = this.manager.getInventory(event.whoClicked.uniqueId) as? InventoryImpl ?: return
        if (event.inventory.holder != inventory.inventory.holder) return

        val slotHandler = inventory.slotClickHandlers[event.slot]
        if (slotHandler != null) {
            slotHandler.invoke(event)
            return
        }

        inventory.onGlobalClick?.invoke(event)
    }

    @EventHandler
    fun handleInventoryClose(event: InventoryCloseEvent) {
        val inventory = this.manager.getInventory(event.player.uniqueId) as? InventoryImpl ?: return
        if (event.inventory.holder != inventory.inventory.holder) return

        inventory.onClose?.invoke(event)

        this.manager.unregister(event.player.uniqueId)
    }

    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        this.manager.unregister(event.player.uniqueId)
    }
}