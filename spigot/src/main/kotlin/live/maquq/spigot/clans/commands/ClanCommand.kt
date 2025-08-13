package live.maquq.spigot.clans.commands

import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import org.bukkit.entity.Player

@Command(name = "clan")
class ClanCommand {

    @Execute
    fun handle(@Context player: Player) {
        player.sendMessage("working!")
    }
}