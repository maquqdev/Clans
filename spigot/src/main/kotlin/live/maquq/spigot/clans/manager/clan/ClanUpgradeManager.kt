package live.maquq.spigot.clans.manager.clan

import live.maquq.api.common.UpgradeType
import live.maquq.api.user.clan.Clan
import live.maquq.api.util.ItemUtil
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import org.bukkit.entity.Player

enum class UpgradeResult {
    SUCCESS,
    MAXED,
    INVALID_STEP,
    INSUFFICIENT_ITEMS
}

class ClanUpgradeManager(
    private val mainConfig: PluginConfiguration
) {

    fun upgrade(clan: Clan, type: UpgradeType, player: Player): UpgradeResult {
        val settings = this.mainConfig.clanSettings

        return when (type) {
            UpgradeType.SIZE -> {
                if (clan.maxSize >= settings.maxSize) return UpgradeResult.MAXED

                val currentLevel = clan.maxSize - settings.defaultSize
                val nextLevelIndex = if (currentLevel < 0) 0 else currentLevel

                val costs = settings.sizeUpgradeCosts
                val cost = if (costs.isEmpty()) 0
                    else if (nextLevelIndex < costs.size) costs[nextLevelIndex]
                    else costs.last()

                if (cost > 0) {
                    val hasPaid = ItemUtil.takeItems(player, settings.item, cost)
                    if (!hasPaid) UpgradeResult.INSUFFICIENT_ITEMS
                }

                clan.maxSize++
                UpgradeResult.SUCCESS
            }

            UpgradeType.POINTS_MULTIPLE -> {
                val defaultMul = settings.defaultPointsMultiplier
                val maxMul = settings.maxPointsMultiplier

                val effectiveStep = settings.pointsMultiplierStep
                    .coerceIn(settings.pointsMultiplierStepMin, settings.pointsMultiplierStepMax)

                if (effectiveStep <= 0.0) return UpgradeResult.INVALID_STEP
                if (clan.pointsMultiplier >= maxMul - 1e-9) return UpgradeResult.MAXED

                val currentLevelDouble = ((clan.pointsMultiplier - defaultMul) / effectiveStep).coerceAtLeast(0.0)
                val nextLevelIndex = currentLevelDouble.toInt()

                val costs = settings.pointsMultipleUpgradeCosts
                val rawCost = if (costs.isEmpty()) 0 else if (nextLevelIndex < costs.size) costs[nextLevelIndex] else costs.last()
                val cost = if (rawCost < 0) 0 else rawCost

                if (cost > 0) {
                    val hasPaid = ItemUtil.takeItems(player, settings.item, cost)
                    if (!hasPaid) return UpgradeResult.INSUFFICIENT_ITEMS
                }

                val newMul = (clan.pointsMultiplier + effectiveStep).coerceAtMost(maxMul)
                clan.pointsMultiplier = kotlin.math.round(newMul * 1000.0) / 1000.0
                UpgradeResult.SUCCESS
            }
        }
    }
}