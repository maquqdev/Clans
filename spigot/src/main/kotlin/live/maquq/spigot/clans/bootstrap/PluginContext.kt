package live.maquq.spigot.clans.bootstrap

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.LiteCommands
import kotlinx.coroutines.CoroutineScope
import live.maquq.api.data.DataSource
import live.maquq.api.user.points.Points
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.Config
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.clans.manager.points.PointsManager
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class PluginContext(
    val plugin: JavaPlugin,
    val logger: BukkitLogger,
    val scope: CoroutineScope
) {
    lateinit var miniText: MiniText

    lateinit var mainConfig: Config<PluginConfiguration>
    lateinit var guiConfig: Config<GuiConfiguration>

    lateinit var dataSource: DataSource
    lateinit var points: Points

    lateinit var inventoryManager: InventoryManager
    lateinit var userManager: UserManager
    lateinit var clanManager: ClanManager
    lateinit var pointsManager: PointsManager

    var liteCommands: LiteCommands<CommandSender>? = null
    val scheduledTaskIds: MutableList<Int> = mutableListOf()
}