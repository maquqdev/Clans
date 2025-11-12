package live.maquq.spigot.clans.menu

import com.bruhdows.minitext.MiniText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.api.common.UpgradeType
import live.maquq.api.user.clan.Clan
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.configuration.impl.ClanSettings
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.clans.manager.clan.ClanUpgradeManager
import live.maquq.spigot.clans.manager.clan.UpgradeResult
import live.maquq.spigot.gui.api.InteractiveInventory
import live.maquq.spigot.gui.builder.ItemBuilder
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.entity.Player
import kotlin.math.round

class ClanUpgradeMenu(
    private val inventoryManager: InventoryManager,
    private val miniText: MiniText,
    private val guiConfiguration: GuiConfiguration,
    private val mainConfig: PluginConfiguration,
    private val clanManager: ClanManager,
    private val userManager: UserManager,
    private val scope: CoroutineScope
) {

    private val upgradeManager = ClanUpgradeManager(mainConfig, clanManager)

    fun open(player: Player) {
        val cfg = this.guiConfiguration.upgradeMenu
        val size = (cfg.rows.coerceIn(1, 6)) * 9
        val menu = this.inventoryManager.create(
            size,
            this.miniText.deserialize(cfg.title).component()
        )

        menu.onGlobalClick { it.isCancelled = true }

        val bg = ItemBuilder(cfg.background, this.miniText).name("").build()
        menu.fill(bg)

        menu.setItem(
            cfg.backItem.slot,
            ItemBuilder(cfg.backItem.material, this.miniText)
                .name(cfg.backItem.title)
                .build()
        ).onClick {
            it.isCancelled = true
            ClanMenu(
                inventoryManager,
                miniText,
                guiConfiguration,
                clanManager,
                userManager,
                mainConfig,
                scope
            ).open(player)
        }

        this.renderLoading(menu, cfg)

        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val clanTag = user.clanTag ?: run {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let { msg ->
                    player.sendMessage(msg)
                }
                return@launch
            }
            val clan = clanManager.getClan(clanTag) ?: return@launch

            renderForClan(menu, clan)
        }

        menu.open(player)
    }

    private fun renderLoading(menu: InteractiveInventory, cfg: GuiConfiguration.UpgradeMenu) {
        val loadingLore = listOf("[gray]Ładowanie danych...")
        val sizeItem = ItemBuilder(cfg.sizeItem.material, miniText)
            .name(cfg.sizeItem.title)
            .lore(loadingLore)
            .build()
        val pointsItem = ItemBuilder(cfg.pointsItem.material, miniText)
            .name(cfg.pointsItem.title)
            .lore(loadingLore)
            .build()
        menu.setItem(cfg.sizeItem.slot, sizeItem)
        menu.setItem(cfg.pointsItem.slot, pointsItem)
    }

    private fun renderForClan(menu: InteractiveInventory, clan: Clan) {
        val cfg = guiConfiguration.upgradeMenu
        val settings = mainConfig.clanSettings

        // SIZE
        val sizeMaxed = clan.maxSize >= settings.maxSize
        val sizePlaceholders = mapOf(
            "[CURRENT]" to clan.maxSize.toString(),
            "[NEXT]" to (if (sizeMaxed) clan.maxSize else (clan.maxSize + 1)).toString(),
            "[MAX]" to settings.maxSize.toString(),
            "[COST]" to nextSizeCost(clan, settings).toString(),
            "[ITEM]" to settings.item.type.name
        )
        val sizeTitle = applyPlaceholders(if (sizeMaxed) cfg.sizeItem.disabledTitle else cfg.sizeItem.title, sizePlaceholders)
        val sizeLore = (if (sizeMaxed) cfg.sizeItem.disabledLore else cfg.sizeItem.lore)
            .map { applyPlaceholders(it, sizePlaceholders) }
        val sizeStack = ItemBuilder(cfg.sizeItem.material, miniText)
            .name(sizeTitle)
            .lore(sizeLore)
            .build()
        menu.setItem(cfg.sizeItem.slot, sizeStack).onClick {
            it.isCancelled = true
            if (sizeMaxed) return@onClick
            val player = it.whoClicked as Player
            val result = upgradeManager.upgrade(clan, UpgradeType.SIZE, player)
            when (result) {
                UpgradeResult.SUCCESS -> {
                    scope.launch { clanManager.saveClan(clan) }
                    renderForClan(menu, clan)
                }
                UpgradeResult.INSUFFICIENT_ITEMS -> {
                    val cost = nextSizeCost(clan, settings)
                    val msg = mainConfig.messages.notEnoughItemsToUpgrade
                        .replace("[COST]", cost.toString())
                        .replace("[ITEM]", settings.item.type.name)
                    miniText.deserialize(msg).component().let { c -> player.sendMessage(c) }
                    player.closeInventory()
                }
                else -> { }
            }
        }

        // POINTS_MULTIPLE
        val step = step()
        val epsilon = 1e-9
        val pointsMaxed = clan.pointsMultiplier >= settings.maxPointsMultiplier - epsilon
        val nextPoints = round(((clan.pointsMultiplier + step).coerceAtMost(settings.maxPointsMultiplier)) * 1000.0) / 1000.0
        val pointsPlaceholders = mapOf(
            "[CURRENT]" to formatDouble(clan.pointsMultiplier),
            "[NEXT]" to formatDouble(nextPoints),
            "[STEP]" to formatDouble(step),
            "[MAX]" to formatDouble(settings.maxPointsMultiplier),
            "[COST]" to nextPointsCost(clan, settings).toString(),
            "[ITEM]" to settings.item.type.name
        )
        val pointsTitle = applyPlaceholders(if (pointsMaxed) cfg.pointsItem.disabledTitle else cfg.pointsItem.title, pointsPlaceholders)
        val pointsLore = (if (pointsMaxed) cfg.pointsItem.disabledLore else cfg.pointsItem.lore)
            .map { applyPlaceholders(it, pointsPlaceholders) }
        val pointsStack = ItemBuilder(cfg.pointsItem.material, miniText)
            .name(pointsTitle)
            .lore(pointsLore)
            .build()
        menu.setItem(cfg.pointsItem.slot, pointsStack).onClick {
            it.isCancelled = true
            if (pointsMaxed) return@onClick
            val player = it.whoClicked as Player
            val result = upgradeManager.upgrade(clan, UpgradeType.POINTS_MULTIPLE, player)
            when (result) {
                UpgradeResult.SUCCESS -> {
                    scope.launch { clanManager.saveClan(clan) }
                    renderForClan(menu, clan)
                }
                UpgradeResult.INSUFFICIENT_ITEMS -> {
                    val cost = nextPointsCost(clan, settings)
                    val msg = mainConfig.messages.notEnoughItemsToUpgrade
                        .replace("[COST]", cost.toString())
                        .replace("[ITEM]", settings.item.type.name)
                    miniText.deserialize(msg).component().let { c -> player.sendMessage(c) }
                    player.closeInventory()
                }
                else -> {
                    // i have nothiiiingg
                }
            }
        }
    }

    private fun applyPlaceholders(text: String, placeholders: Map<String, String>): String {
        var result = text
        for ((k, v) in placeholders) {
            result = result.replace(k, v)
        }
        return result
    }

    private fun nextSizeCost(clan: Clan, settings: ClanSettings): Int {
        val currentLevel = clan.maxSize - settings.defaultSize
        val nextLevelIndex = if (currentLevel < 0) 0 else currentLevel
        val costs = settings.sizeUpgradeCosts
        return if (costs.isEmpty()) 0 else if (nextLevelIndex < costs.size) costs[nextLevelIndex] else costs.last()
    }

    private fun nextPointsCost(clan: Clan, settings: ClanSettings): Int {
        val defaultMul = settings.defaultPointsMultiplier
        val currentLevelDouble = ((clan.pointsMultiplier - defaultMul) / step()).coerceAtLeast(0.0)
        val nextLevelIndex = currentLevelDouble.toInt()
        val costs = settings.pointsMultipleUpgradeCosts
        val rawCost = if (costs.isEmpty()) 0 else if (nextLevelIndex < costs.size) costs[nextLevelIndex] else costs.last()
        return if (rawCost < 0) 0 else rawCost
    }

    private fun step(): Double {
        val s = mainConfig.clanSettings
        return s.pointsMultiplierStep.coerceIn(s.pointsMultiplierStepMin, s.pointsMultiplierStepMax)
    }

    private fun formatDouble(value: Double): String {
        return String.format("%.2f", value)
    }
}