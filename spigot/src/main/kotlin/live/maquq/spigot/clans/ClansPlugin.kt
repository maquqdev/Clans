package live.maquq.spigot.clans

import com.bruhdows.minitext.MiniText
import com.bruhdows.minitext.formatter.FormatterType
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import kotlinx.coroutines.*
import live.maquq.api.DataSource
import live.maquq.spigot.clans.commands.ClanCommand
import live.maquq.spigot.clans.commands.handler.InsufficientPermissionHandler
import live.maquq.spigot.clans.commands.handler.InvalidUsageHandler
import live.maquq.spigot.clans.configuration.Config
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.configuration.impl.StorageType
import live.maquq.spigot.clans.listener.PlayerJoinListener
import live.maquq.spigot.clans.manager.ClanManager
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.storage.impl.FlatDataSource
import live.maquq.storage.impl.MongoDataSource
import live.maquq.storage.impl.MySqlDataSource
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ClansPlugin : JavaPlugin() {

    /*
            TODO
        * Komendy:
            * /klan stworz <name>
            * /klan dolacz <name>
            * /klan opusc
            * /klan info <klan>
            * /klan usun [potwierdz]
            * /klan ustawienia
            * /klan ulepsz
       Kilka systemów punktów
       Title po zabójstwie

       ehhh zas te not null safety kod... po co ten kotlin???????
     */

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + this.job)

    private val logger: BukkitLogger = BukkitLogger(this, true) //TODO: Get debug mode from config, don't hardcode

    private lateinit var miniText: MiniText

    private lateinit var dataSource: DataSource
    private lateinit var mainConfig: Config<PluginConfiguration>

    private lateinit var liteCommands: LiteCommands<CommandSender>

    private lateinit var userManager: UserManager
    private lateinit var clanManager: ClanManager

    override fun onEnable() {
        this.logger.info(
            """\n
             ／l、         
           （ﾟ､ ｡７           Thanks for using
            l、ﾞ~ヽ              my plugin!
            じしf_, )ノ         maquq @ 2025
        """.trimIndent()
        )
        this.miniText = MiniText.builder()
            .enableFormatter(FormatterType.LEGACY, FormatterType.NAMED_COLORS, FormatterType.HEX, FormatterType.NEW_LINES, FormatterType.DECORATIONS)
            .build()

        this.mainConfig = Config(
            PluginConfiguration::class.java,
            File(this.dataFolder, "config.yml"),
            this.logger
        )

        this.dataSource = initializeDataSource(mainConfig.get)

        if (!this.setupDataSource()) {
            this.logger.error("Connection to database failed. Change database login credentials :)")
            this.server.pluginManager.disablePlugin(this)
            return
        }

        this.setupManagers()
        this.registerIntegrations()

        this.loadClansToCache()
        this.loadCommands()

        this.logger.info("Plugin has been successfully loaded!")
    }

    override fun onDisable() {
        this.mainConfig.shutdown()
        this.dataSource.disconnect()
        this.logger.shutdown()
        this.job.cancel() //need to cancel slur...🌹🌺🌺

        super.getLogger().info("Plugin has been successfully disabled!")
    }

    private fun initializeDataSource(config: PluginConfiguration): DataSource {
        return when (config.storage) {
            StorageType.FLAT -> FlatDataSource(this.dataFolder)
            StorageType.MYSQL -> MySqlDataSource(
                mapOf(
                    "host" to config.mysql.host,
                    "port" to config.mysql.port,
                    "database" to config.mysql.database,
                    "username" to config.mysql.username,
                    "password" to config.mysql.password
                )
            )

            StorageType.MONGODB -> MongoDataSource(config.mongo.connectionString)
        }
    }

    private fun setupDataSource(): Boolean {
        return runCatching {
            this.dataSource.connect()
            this.logger.info("Successfully connected to database! (${mainConfig.get.storage})")
        }.onFailure {
            this.logger.error("Cannot connect to database, check configuration please!", it)
        }.isSuccess
    }

    private fun loadClansToCache() {
        scope.launch { clanManager.preloadAllClansToCache() }
    }

    private fun setupManagers() {
        this.clanManager = ClanManager(
            this.dataSource,
            this.logger
        )
        this.userManager = UserManager(
            this.dataSource,
            this.clanManager,
            this.logger,
            scope
        )
    }

    private fun loadCommands() {
        this.liteCommands = LiteBukkitFactory.builder()
            .missingPermission(
                InsufficientPermissionHandler(
                    this.miniText,
                    this.mainConfig.get
                )
            )
            .invalidUsage(
                InvalidUsageHandler(
                    this.miniText,
                    this.mainConfig.get
                )
            )
            .commands(
                ClanCommand()
            )
            .build()
    }

    private fun registerIntegrations() {
        val pluginManager = this.server.pluginManager

        val playerJoinListener = PlayerJoinListener(
            this.userManager,
            this.scope
        )
        pluginManager.registerEvents(playerJoinListener, this)
    }
}