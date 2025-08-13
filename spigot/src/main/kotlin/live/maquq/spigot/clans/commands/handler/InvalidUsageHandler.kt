package live.maquq.spigot.clans.commands.handler

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.handler.result.ResultHandlerChain
import dev.rollczi.litecommands.invalidusage.InvalidUsage
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler
import dev.rollczi.litecommands.invocation.Invocation
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import org.bukkit.command.CommandSender


class InvalidUsageHandler(
    private val miniText: MiniText,
    private val pluginConfiguration: PluginConfiguration
) : InvalidUsageHandler<CommandSender> {

    override fun handle(
        invocation: Invocation<CommandSender>?,
        result: InvalidUsage<CommandSender>?,
        chain: ResultHandlerChain<CommandSender>?
    ) {
        val sender = invocation!!.sender()
        val schematic = result!!.schematic
        if (schematic.isOnlyFirst) {
            val translatedCorrectUsage = miniText.deserialize(
                this.pluginConfiguration.messages.correctUsage
                    .replace(
                        "[CORRECT]",
                        schematic.first()
                    )
            ).component()
            sender.sendMessage(translatedCorrectUsage)
            return
        }

        val translatedCorrectUsage = miniText.deserialize(
            this.pluginConfiguration.messages.correctUsages
        ).component()
        sender.sendMessage(translatedCorrectUsage)
        for (scheme in schematic.all()) {
            sender.sendMessage("- $scheme")
        }
    }
}