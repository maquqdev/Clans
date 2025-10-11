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
        this.scope.launch {
            val targetUser = userManager.getUser(target.uniqueId)
            userManager.sendInfo(
                player = player,
                targetUser = targetUser,
                mainConfig = mainConfig,
                miniText = miniText
            )
        }
    }
}