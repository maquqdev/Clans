package live.maquq.spigot.clans.bootstrap.modules

import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.clans.manager.points.PointsManager
import live.maquq.spigot.gui.manager.InventoryManager

class ManagersModule : PluginModule {
    override val name: String = "Managers"

    override suspend fun enable(ctx: PluginContext) {
        ctx.userManager = UserManager(
            dataSource = ctx.dataSource,
            logger = ctx.logger,
            scope = ctx.scope,
            mainConfig = ctx.mainConfig.get
        )

        ctx.clanManager = ClanManager(
            dataSource = ctx.dataSource,
            userManager = ctx.userManager,
            mainConfig = ctx.mainConfig.get,
            logger = ctx.logger
        )

        ctx.pointsManager = PointsManager(points = ctx.points)

        ctx.inventoryManager = InventoryManager(ctx.plugin).also { it.initialize() }
    }

    override suspend fun disable(ctx: PluginContext) {
    }
}