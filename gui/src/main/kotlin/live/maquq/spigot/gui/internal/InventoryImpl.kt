package live.maquq.spigot.gui.internal

import live.maquq.spigot.gui.api.ClickableItem
import live.maquq.spigot.gui.api.InteractiveInventory
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

internal class InventoryImpl(
    override val inventory: Inventory,
    private val manager: InventoryManager,
    private val javaPlugin: JavaPlugin,
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
        Bukkit.getScheduler().runTask(this.javaPlugin, Runnable {
            player.openInventory(inventory)
            manager.register(player, this)
        })
    }

    override fun setItem(slot: Int, item: ItemStack): ClickableItem {
        this.inventory.setItem(slot, item)
        return ClickableItemImpl(this, slot)
    }
}