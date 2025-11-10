package live.maquq.spigot.clans.menu

import com.bruhdows.minitext.MiniText
import kotlinx.coroutines.CoroutineScope
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.gui.builder.ItemBuilder
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.Material
import org.bukkit.entity.Player

class ClanMenu(
    private val inventoryManager: InventoryManager,
    private val miniText: MiniText,
    private val guiConfiguration: GuiConfiguration,
    private val clanManager: ClanManager,
    private val userManager: UserManager,
    private val mainConfig: PluginConfiguration,
    private val scope: CoroutineScope
) {

    private var minecartPosition = 0
    private val railSlots = listOf(
        0, 9, 18, 27,
        35, 26, 17, 8
    )

    fun open(player: Player) {
        val menu = this.inventoryManager.create(
            4 * 9,
            this.miniText.deserialize(this.guiConfiguration.panel.title).component()
        )

        menu.onGlobalClick {
            it.isCancelled = true
        }

        val backgroundItem = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE, this.miniText)
            .build()
        menu.fill(backgroundItem)

        val railItem = ItemBuilder(Material.RAIL, this.miniText)
            .name("")
            .build()

        listOf(0, 9, 18, 27).forEach { slot ->
            menu.setItem(slot, railItem)
        }

        listOf(8, 17, 26, 35).forEach { slot ->
            menu.setItem(slot, railItem)
        }

        menu.setUpdateTask {
            val prevSlot = railSlots[minecartPosition]
            menu.inventory.setItem(prevSlot, railItem)

            minecartPosition = (minecartPosition + 1) % railSlots.size

            val minecartItem = ItemBuilder(Material.MINECART, this.miniText)
                .name("")
                .build()

            menu.inventory.setItem(
                railSlots[minecartPosition],
                minecartItem
            )
        }

        this.guiConfiguration.upgradePanel.let {
            menu.setItem(
                13,
                ItemBuilder(this.guiConfiguration.upgradePanel.material, this.miniText)
                    .name(this.guiConfiguration.upgradePanel.title)
                    .lore(this.guiConfiguration.upgradePanel.lore)
                    .build()
            ).onClick {
                it.isCancelled = true

                ClanUpgradeMenu(
                    this.inventoryManager,
                    this.miniText,
                    this.guiConfiguration,
                    this.mainConfig,
                    this.clanManager,
                    this.userManager,
                    this.scope
                ).open(player)
            }
        }


        menu.open(player)
    }
}