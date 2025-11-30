package live.maquq.spigot.clans.menu

import com.bruhdows.minitext.MiniText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.api.common.ClanRole
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

        // PvP toggle
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val tag = user.clanTag ?: return@launch
            val clan = clanManager.getClan(tag) ?: return@launch
            val role = clan.members[user.uuid]

            val canToggle = role != null && hasRoleAtLeast(role, clan.pvpEditMinRole)
            val toggleMaterial = if (clan.pvpEnabled) Material.LIME_DYE else Material.GRAY_DYE
            val toggleTitle = if (clan.pvpEnabled) "[green]PvP: WŁĄCZONE" else "[red]PvP: WYŁĄCZONE"
            val toggleLore =
                if (canToggle) listOf("[gray]Kliknij, aby przełączyć") else listOf("[gray]Brak uprawnień do zmiany")

            menu.setItem(
                21,
                ItemBuilder(toggleMaterial, miniText)
                    .name(toggleTitle)
                    .lore(toggleLore)
                    .build()
            ).onClick {
                it.isCancelled = true
                if (!canToggle) return@onClick
                scope.launch {
                    val user = userManager.getUser(player.uniqueId)
                    val clanTag = user.clanTag ?: return@launch
                    val clan = clanManager.getClan(clanTag) ?: return@launch
                    val clanMembers = clan.members[user.uuid]
                    if (clanMembers == null || !hasRoleAtLeast(clanMembers, clan.pvpEditMinRole)) return@launch
                    clan.pvpEnabled = !clan.pvpEnabled
                    clanManager.saveClan(clan)
                    val msg =
                        if (clan.pvpEnabled) mainConfig.messages.pvpEnabledNow else mainConfig.messages.pvpDisabledNow
                    miniText.deserialize(msg).component().let { message -> player.sendMessage(message) }
                    open(player)
                }
            }

            val leaderOnly = role == ClanRole.LEADER
            val minRoleItem = guiConfiguration.panel.minRoleItem
            val roleLore =
                if (leaderOnly) minRoleItem.minRoleLore else minRoleItem.minRoleLoreOnlyLeader

            menu.setItem(
                23,
                ItemBuilder(minRoleItem.minRoleMaterial, miniText)
                    .name(minRoleItem.minRoleTitle
                        .replace(
                            "[MIN-ROLE]",
                            clan.pvpEditMinRole.name)
                    )
                    .lore(roleLore)
                    .build()
            ).onClick { event ->
                event.isCancelled = true
                if (!leaderOnly) return@onClick
                scope.launch {
                    val user = userManager.getUser(player.uniqueId)
                    val clanTag = user.clanTag ?: return@launch

                    val clan = clanManager.getClan(clanTag) ?: return@launch
                    val clanRole = clan.members[user.uuid]
                    if (clanRole != ClanRole.LEADER) return@launch

                    clan.pvpEditMinRole = nextRole(clan.pvpEditMinRole)
                    clanManager.saveClan(clan)
                    miniText.deserialize(mainConfig.messages.pvpMinRoleUpdated.replace("[ROLE]", clan.pvpEditMinRole.name))
                        .component().let { message ->
                            player.sendMessage(message)
                        }
                    open(player)
                }
            }
        }

        menu.open(player)
    }

    private fun hasRoleAtLeast(current: ClanRole, minimum: ClanRole): Boolean {
        fun weight(role: ClanRole) = when (role) {
            ClanRole.LEADER -> 3
            ClanRole.COLEADER -> 2
            ClanRole.MEMBER -> 1
        }
        return weight(current) >= weight(minimum)
    }

    private fun nextRole(role: ClanRole): ClanRole {
        return when (role) {
            ClanRole.MEMBER -> ClanRole.COLEADER
            ClanRole.COLEADER -> ClanRole.LEADER
            ClanRole.LEADER -> ClanRole.MEMBER
        }
    }
}