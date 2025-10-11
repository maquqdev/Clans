package live.maquq.spigot.gui.internal

import live.maquq.spigot.gui.api.ClickableItem
import live.maquq.spigot.gui.api.InteractiveInventory
import org.bukkit.event.inventory.InventoryClickEvent

internal class ClickableItemImpl(
    private val parent: InventoryImpl,
    private val slot: Int
) : ClickableItem {

    override fun onClick(handler: (InventoryClickEvent) -> Unit): InteractiveInventory {
        this.parent.slotClickHandlers[this.slot] = handler
        return this.parent
    }
}