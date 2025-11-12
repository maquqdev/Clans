package live.maquq.spigot.clans.bootstrap.modules

import kotlinx.coroutines.launch
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule

class PreloadModule : PluginModule {
    override val name: String = "Preload"

    override suspend fun enable(ctx: PluginContext) {
        ctx.scope.launch {
            ctx.clanManager.preloadAllClansToCache()
            ctx.userManager.loadTopUsers()
        }
    }

    override suspend fun disable(ctx: PluginContext) { }
}