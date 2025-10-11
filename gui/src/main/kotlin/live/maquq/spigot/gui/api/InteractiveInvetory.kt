package live.maquq.spigot.gui.api

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface InteractiveInventory {

    val inventory: Inventory

    fun onGlobalClick(consumer: (InventoryClickEvent) -> Unit)
    fun onClose(consumer: (InventoryCloseEvent) -> Unit)
    fun setUpdateTask(task: Runnable)
    fun open(player: Player)
    fun setItem(slot: Int, item: ItemStack): ClickableItem

    fun setItem(slot: Int, material: Material, builder: ItemMeta.() -> Unit = {}): ClickableItem {
        val item = ItemStack(material).apply {
            itemMeta = itemMeta?.apply(builder)
        }
        return setItem(slot, item)
    }

    fun setItem(slot: Int, baseItem: ItemStack, builder: ItemMeta.() -> Unit): ClickableItem {
        val item = baseItem.clone().apply {
            itemMeta = itemMeta?.apply(builder)
        }
        return setItem(slot, item)
    }

    fun fill(item: ItemStack) {
        for (i in 0 until inventory.size)
            inventory.setItem(i, item.clone())
    }
}