package live.maquq.spigot.clans.bootstrap.modules

import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.commands.ClanCommand
import live.maquq.spigot.clans.commands.PlayerCommand
import live.maquq.spigot.clans.commands.handler.InsufficientPermissionHandler
import live.maquq.spigot.clans.commands.handler.InvalidUsageHandler
import org.bukkit.command.CommandSender

class CommandsModule : PluginModule {
    override val name: String = "Commands"

    override suspend fun enable(ctx: PluginContext) {
        val builder = LiteBukkitFactory.builder()
            .missingPermission(
                InsufficientPermissionHandler(
                    ctx.miniText,
                    ctx.mainConfig.get
                )
            )
            .invalidUsage(
                InvalidUsageHandler(
                    ctx.miniText,
                    ctx.mainConfig.get
                )
            )
            .commands(
                ClanCommand(
                    miniText = ctx.miniText,
                    mainConfig = ctx.mainConfig.get,
                    scope = ctx.scope,
                    clanManager = ctx.clanManager,
                    userManager = ctx.userManager,
                    inventoryManager = ctx.inventoryManager,
                    guiConfiguration = ctx.guiConfig.get
                ),
                PlayerCommand(
                    miniText = ctx.miniText,
                    scope = ctx.scope,
                    mainConfig = ctx.mainConfig.get,
                    userManager = ctx.userManager
                )
            )

        val commands: LiteCommands<CommandSender> = builder.build()
        ctx.liteCommands = commands
    }

    override suspend fun disable(ctx: PluginContext) {
        ctx.liteCommands!!.unregister()
        ctx.liteCommands = null
    }
}