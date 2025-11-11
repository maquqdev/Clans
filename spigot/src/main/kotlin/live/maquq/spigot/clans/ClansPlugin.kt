package live.maquq.spigot.clans

import com.bruhdows.minitext.MiniText
import com.bruhdows.minitext.formatter.FormatterType
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import kotlinx.coroutines.*
import live.maquq.api.data.DataSource
import live.maquq.api.user.points.Points
import live.maquq.spigot.clans.bootstrap.ModuleInitializer
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.modules.CommandsModule
import live.maquq.spigot.clans.bootstrap.modules.ConfigModule
import live.maquq.spigot.clans.bootstrap.modules.DataSourceModule
import live.maquq.spigot.clans.bootstrap.modules.ListenersModule
import live.maquq.spigot.clans.bootstrap.modules.ManagersModule
import live.maquq.spigot.clans.bootstrap.modules.PointsModule
import live.maquq.spigot.clans.bootstrap.modules.PreloadModule
import live.maquq.spigot.clans.bootstrap.modules.TasksModule
import live.maquq.spigot.clans.bootstrap.modules.VersionCheckModule
import live.maquq.spigot.gui.manager.InventoryManager
import live.maquq.spigot.clans.commands.ClanCommand
import live.maquq.spigot.clans.commands.PlayerCommand
import live.maquq.spigot.clans.commands.handler.InsufficientPermissionHandler
import live.maquq.spigot.clans.commands.handler.InvalidUsageHandler
import live.maquq.spigot.clans.configuration.Config
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.configuration.impl.PointsType
import live.maquq.spigot.clans.configuration.impl.StorageType
import live.maquq.spigot.clans.listener.PlayerDeathListener
import live.maquq.spigot.clans.listener.PlayerInteractEntityListener
import live.maquq.spigot.clans.listener.PlayerJoinListener
import live.maquq.spigot.clans.listener.PlayerQuitListener
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.points.PointsManager
import live.maquq.spigot.clans.manager.points.impl.CompositePoints
import live.maquq.spigot.clans.manager.points.impl.SkillBasedPoints
import live.maquq.spigot.clans.task.DataSaveTask
import live.maquq.storage.impl.FlatDataSource
import live.maquq.storage.impl.MongoDataSource
import live.maquq.storage.impl.MySqlDataSource
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ClansPlugin : JavaPlugin() {

    /*
        TODO

        Save wszystkich userow i clan

       TODO Załadować dana il. użytkowników do topki (np. top 50)
       TODO VaultUnlocked hook żeby robić upgrade size klanu
       TODO Możliwość knockowania klanowiczów bez dmg?
       TODO Dokończyć API do klanów (moduł API)

       readme
     */

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + this.job)

    private val logger: BukkitLogger = BukkitLogger(
        this,
        true
    )

    private lateinit var initializer: ModuleInitializer
    private lateinit var ctx: PluginContext

    override fun onEnable() {
        val startTime = System.currentTimeMillis()
        this.logger.info(
            """
                
             ／l、         
           （ﾟ､ ｡７           Thanks for using
            l、ﾞ~ヽ              my plugin!
            じしf_, )ノ         maquq @ 2025
        """.trimIndent()
        )

        this.ctx = PluginContext(
            this,
            this.logger,
            this.scope
        )
        this.initializer = ModuleInitializer(
            listOf(
                ConfigModule(),
                DataSourceModule(),
                PointsModule(),
                ManagersModule(),
                ListenersModule(),
                PreloadModule(),
                TasksModule(),
                CommandsModule(),
                VersionCheckModule()
            ),
            this.logger
        )

        runBlocking { initializer.enableAll(ctx) }

        this.logger.info("Plugin has been successfully loaded in ${System.currentTimeMillis() - startTime}ms!")
    }

    override fun onDisable() {
        runBlocking {
            if (this@ClansPlugin::initializer.isInitialized && this@ClansPlugin::ctx.isInitialized) {
                initializer.disableAll(ctx)
            }
        }
        this.logger.shutdown()
        this.job.cancel()

        super.getLogger().info("Plugin has been successfully disabled!")
    }
}