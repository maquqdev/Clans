package live.maquq.spigot.gui.api

import org.bukkit.event.inventory.InventoryClickEvent

interface ClickableItem {

    fun onClick(handler: (InventoryClickEvent) -> Unit): InteractiveInventory
}