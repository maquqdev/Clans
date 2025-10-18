package live.maquq.spigot.gui.manager

import live.maquq.spigot.gui.listener.InventoryListener
import live.maquq.spigot.gui.api.InteractiveInventory
import live.maquq.spigot.gui.internal.InventoryImpl
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InventoryManager(private val plugin: JavaPlugin) {

    private val activeInventories: MutableMap<UUID, InteractiveInventory> = ConcurrentHashMap()
    private val updateTasks: MutableMap<UUID, Int> = ConcurrentHashMap()

    fun initialize() {
        Bukkit.getPluginManager().registerEvents(InventoryListener(this), this.plugin)
    }

    fun create(size: Int, title: Component): InteractiveInventory {
        val bukkitInventory = Bukkit.createInventory(null, size, title)
        return InventoryImpl(
            bukkitInventory,
            this,
            this.plugin
        )
    }

    internal fun register(player: Player, inventory: InteractiveInventory) {
        this.activeInventories[player.uniqueId] = inventory

        (inventory as? InventoryImpl)?.onUpdate?.let { task ->
            val taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, task, 0L, 20L).taskId
            this.updateTasks[player.uniqueId] = taskId
        }
    }

    internal fun unregister(uuid: UUID) {
        this.activeInventories.remove(uuid)
        this.updateTasks.remove(uuid)?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
        }
    }

    internal fun getInventory(uuid: UUID): InteractiveInventory? {
        return this.activeInventories[uuid]
    }
}