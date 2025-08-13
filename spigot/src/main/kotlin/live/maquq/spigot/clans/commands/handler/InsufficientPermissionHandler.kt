package live.maquq.spigot.clans.commands.handler

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.handler.result.ResultHandlerChain
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.permission.MissingPermissions
import dev.rollczi.litecommands.permission.MissingPermissionsHandler
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import org.bukkit.command.CommandSender


class InsufficientPermissionHandler(
    private val miniText: MiniText,
    private val pluginConfiguration: PluginConfiguration
) : MissingPermissionsHandler<CommandSender> {

    override fun handle(
        invocation: Invocation<CommandSender>,
        missingPermissions: MissingPermissions,
        chain: ResultHandlerChain<CommandSender>
    ) {
        val permissions = missingPermissions.asJoinedText()
        val sender = invocation.sender()

        val translated = miniText.deserialize(
            this.pluginConfiguration.messages.noPermission
                .replace("[PERMISSION]", permissions)
        ).component()
        sender.sendMessage(translated)
    }
}