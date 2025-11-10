package live.maquq.spigot.clans.bootstrap.modules

import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.configuration.impl.PointsType
import live.maquq.spigot.clans.manager.points.impl.CompositePoints
import live.maquq.spigot.clans.manager.points.impl.SkillBasedPoints

class PointsModule : PluginModule {
    override val name: String = "Points"

    override suspend fun enable(ctx: PluginContext) {
        val cfg = ctx.mainConfig.get
        ctx.points = when (cfg.clanSettings.pointsConfiguration.pointsType) {
            PointsType.COMPOSITE -> CompositePoints(cfg.clanSettings.proportionalPointsConfiguration)
            PointsType.SKILL_BASED -> SkillBasedPoints(cfg.clanSettings.skillBasedPointsConfiguration)
        }
    }

    override suspend fun disable(ctx: PluginContext) {
    }
}