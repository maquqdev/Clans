package live.maquq.spigot.clans.menu

import com.bruhdows.minitext.MiniText
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.gui.builder.ItemBuilder
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.Material
import org.bukkit.entity.Player

class ClanUpgradeMenu(
    private val inventoryManager: InventoryManager,
    private val miniText: MiniText,
    private val guiConfiguration: GuiConfiguration
) {

    fun open(player: Player) {
        val menu = this.inventoryManager.create(
            3 * 9,
            this.miniText.deserialize(this.guiConfiguration.panel.title).component()
        )

        menu.onGlobalClick {
            it.isCancelled = true
        }

        menu.setItem(
            13,
            ItemBuilder(Material.BOOK, this.miniText)
                .name("no ogolnie to jeszcze nie zrobilem tego")
                .build()
        )

        menu.open(player)
    }

}