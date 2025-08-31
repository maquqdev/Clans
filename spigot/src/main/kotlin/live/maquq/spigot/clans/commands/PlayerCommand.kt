package live.maquq.spigot.clans.commands

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import org.bukkit.entity.Player

@Command(name = "gracz")
class PlayerCommand(
    private val scope: CoroutineScope,
    private val userManager: UserManager,
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration
) {

    @Execute
    fun execute(
        @Context player: Player,
        @Arg("gracz") target: Player,
    ) {
        scope.launch {
            userManager.getUser(target.uniqueId).run {
                val clanTag = this.clanTag ?: "BRAK"
                val kdFormatted = if (deaths > 0)
                    String.format("%.2f", kills.toDouble() / deaths)
                else
                    kills.toString()

                val message = mainConfig.messages.playerInfo
                    .replace("[PLAYER]", target.name)
                    .replace("[TAG]", clanTag)
                    .replace("[POINTS]", points.toString())
                    .replace("[DEATHS]", deaths.toString())
                    .replace("[KILLS]", kills.toString())
                    .replace("[KD]", kdFormatted)

                miniText.deserialize(message).component().let {
                    player.sendMessage(it)
                }
            }
        }
    }
}