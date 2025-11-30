package live.maquq.spigot.clans.bootstrap.modules

import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.placeholder.ClansExpansion
import org.bukkit.Bukkit

class PlaceholdersModule : PluginModule {
    override val name: String = "Placeholders"

    override suspend fun enable(ctx: PluginContext) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            ctx.logger.info("PlaceholderAPI not found, skipping placeholder registration")
            return
        }

        val expansion = ClansExpansion(
            userManager = ctx.userManager,
            clanManager = ctx.clanManager
        )

        if (expansion.register()) {
            ctx.logger.info("Successfully registered Clans placeholder expansion")
        } else {
            ctx.logger.error("Failed to register Clans placeholder expansion")
        }
    }

    override suspend fun disable(ctx: PluginContext) {
    }
}