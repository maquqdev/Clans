package live.maquq.spigot.clans.gui.internal

import live.maquq.spigot.clans.gui.api.ClickableItem
import live.maquq.spigot.clans.gui.api.InteractiveInventory
import live.maquq.spigot.clans.gui.manager.InventoryManager
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

internal class InventoryImpl(
    override val inventory: Inventory,
    private val manager: InventoryManager
) : InteractiveInventory {

    internal val slotClickHandlers: MutableMap<Int, (InventoryClickEvent) -> Unit> = mutableMapOf()
    internal var onGlobalClick: ((InventoryClickEvent) -> Unit)? = null
    internal var onClose: ((InventoryCloseEvent) -> Unit)? = null
    internal var onUpdate: Runnable? = null

    override fun onGlobalClick(consumer: (InventoryClickEvent) -> Unit) {
        this.onGlobalClick = consumer
    }

    override fun onClose(consumer: (InventoryCloseEvent) -> Unit) {
        this.onClose = consumer
    }

    override fun setUpdateTask(task: Runnable) {
        this.onUpdate = task
    }

    override fun open(player: Player) {
        this.manager.register(player, this)
        player.openInventory(this.inventory)
    }

    override fun setItem(slot: Int, item: ItemStack): ClickableItem {
        this.inventory.setItem(slot, item)
        return ClickableItemImpl(this, slot)
    }
}