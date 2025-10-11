package live.maquq.spigot.clans

import com.bruhdows.minitext.MiniText
import com.bruhdows.minitext.formatter.FormatterType
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import kotlinx.coroutines.*
import live.maquq.api.data.DataSource
import live.maquq.api.user.points.Points
import live.maquq.spigot.gui.manager.InventoryManager
import live.maquq.spigot.clans.commands.ClanCommand
import live.maquq.spigot.clans.commands.PlayerCommand
import live.maquq.spigot.clans.commands.handler.InsufficientPermissionHandler
import live.maquq.spigot.clans.commands.handler.InvalidUsageHandler
import live.maquq.spigot.clans.configuration.Config
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
import live.maquq.storage.impl.FlatDataSource
import live.maquq.storage.impl.MongoDataSource
import live.maquq.storage.impl.MySqlDataSource
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ClansPlugin : JavaPlugin() {

    /*
        TODO
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

    private lateinit var miniText: MiniText

    private lateinit var dataSource: DataSource
    private lateinit var points: Points

    private lateinit var mainConfig: Config<PluginConfiguration>

    private lateinit var liteCommands: LiteCommands<CommandSender>

    private lateinit var userManager: UserManager
    private lateinit var clanManager: ClanManager
    private lateinit var pointsManager: PointsManager
    private lateinit var inventoryManager: InventoryManager

    override fun onEnable() {
        val startTime = System.currentTimeMillis()
        this.logger.info(
            """\n
             ／l、         
           （ﾟ､ ｡７           Thanks for using
            l、ﾞ~ヽ              my plugin!
            じしf_, )ノ         maquq @ 2025
        """.trimIndent()
        )

        this.miniText = MiniText.builder()
            .enableFormatter(
                FormatterType.LEGACY,
                FormatterType.NAMED_COLORS,
                FormatterType.HEX,
                FormatterType.NEW_LINES,
                FormatterType.DECORATIONS,
                FormatterType.RESET
            )
            .build()

        this.mainConfig = Config(
            PluginConfiguration::class.java,
            File(
                this.dataFolder,
                "config.json"
            ),
            this.logger
        )

        this.dataSource = this.initializeDataSource(mainConfig.get)
        this.points = this.initializePoints(mainConfig.get)

        if (!this.setupDataSource()) {
            this.logger.error("Connection to database failed. Change database login credentials or use FLAT :)")
            this.server.pluginManager.disablePlugin(this)
            return
        }

        this.setupManagers()
        this.registerListeners()
        VersionChecker(
            this,
            logger,
            scope
        ).check()

        this.loadClansToCache()
        this.loadCommands()

        this.logger.info("Plugin has been successfully loaded in ${System.currentTimeMillis() - startTime}ms!")
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

    private fun initializePoints(mainConfig: PluginConfiguration): Points {
        return when (mainConfig.clanSettings.pointsConfiguration.pointsType) {
            PointsType.COMPOSITE -> CompositePoints(mainConfig.clanSettings.proportionalPointsConfiguration)
            PointsType.SKILL_BASED -> SkillBasedPoints(mainConfig.clanSettings.skillBasedPointsConfiguration)
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
        this.scope.launch { clanManager.preloadAllClansToCache() }
    }

    private fun setupManagers() {
        this.userManager = UserManager(
            dataSource = this.dataSource,
            logger = this.logger,
            scope = this.scope,
            mainConfig = this.mainConfig.get
        )

        this.clanManager = ClanManager(
            plugin = this,
            dataSource = this.dataSource,
            userManager = this.userManager,
            mainConfig = this.mainConfig.get,
            logger = this.logger
        )

        this.pointsManager = PointsManager(
            points = this.points
        )

        this.inventoryManager = InventoryManager(
            this
        )

        this.inventoryManager.initialize()
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
                ClanCommand(
                    miniText = this.miniText,
                    mainConfig = this.mainConfig.get,
                    scope = this.scope,
                    clanManager = this.clanManager,
                    userManager = this.userManager,
                    plugin = this
                ),
                PlayerCommand(
                    miniText = this.miniText,
                    scope = this.scope,
                    mainConfig = this.mainConfig.get,
                    userManager = this.userManager
                )
            )
            .build()
    }

    private fun registerListeners() {
        val playerJoinListener = PlayerJoinListener(
            this.userManager,
            this.scope
        )
        val playerQuitListener = PlayerQuitListener(
            this.userManager
        )
        val playerDeathListener = PlayerDeathListener(
            this.scope,
            this.miniText,
            this.mainConfig.get.messages.playerDeath,
            this.pointsManager,
            this.userManager,
            this.clanManager
        )
        val playerInteractEntityListener = PlayerInteractEntityListener(
            this.userManager,
            this.scope,
            this.miniText,
            this.mainConfig.get
        )

        val pluginManager = this.server.pluginManager
        pluginManager.registerEvents(playerJoinListener, this)
        pluginManager.registerEvents(playerQuitListener, this)
        pluginManager.registerEvents(playerDeathListener, this)
        pluginManager.registerEvents(playerInteractEntityListener, this)
    }
}