package live.maquq.spigot.clans.bootstrap.modules

import live.maquq.spigot.clans.VersionChecker
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule

class VersionCheckModule : PluginModule {
    override val name: String = "VersionCheck"

    override suspend fun enable(ctx: PluginContext) {
        VersionChecker(ctx.plugin, ctx.logger, ctx.scope).check()
    }

    override suspend fun disable(ctx: PluginContext) { /* aha37 */ }
}