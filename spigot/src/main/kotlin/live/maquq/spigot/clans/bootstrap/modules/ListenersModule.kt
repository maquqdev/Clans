package live.maquq.spigot.clans.bootstrap.modules

import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.listener.*

class ListenersModule : PluginModule {
    override val name: String = "Listeners"

    override suspend fun enable(ctx: PluginContext) {
        val plugin = ctx.plugin
        val pm = plugin.server.pluginManager

        pm.registerEvents(PlayerJoinListener(ctx.userManager, ctx.scope), plugin)
        pm.registerEvents(PlayerQuitListener(ctx.userManager), plugin)
        pm.registerEvents(
            PlayerDeathListener(
                ctx.scope,
                ctx.miniText,
                ctx.mainConfig.get.messages.playerDeath,
                ctx.pointsManager,
                ctx.userManager,
                ctx.clanManager
            ),
            plugin
        )
        pm.registerEvents(
            PlayerInteractEntityListener(
                ctx.userManager,
                ctx.scope,
                ctx.miniText,
                ctx.mainConfig.get
            ),
            plugin
        )
        pm.registerEvents(
            EntityDamageEntityListener(
                ctx.userManager,
                ctx.clanManager,
                ctx.scope
            ),
            plugin
        )
    }

    override suspend fun disable(ctx: PluginContext) {
    }
}