package live.maquq.spigot.clans

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import live.maquq.spigot.clans.bootstrap.ModuleInitializer
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.modules.*
import org.bukkit.plugin.java.JavaPlugin

class ClansPlugin : JavaPlugin() {

    /*
        TODO

        Save wszystkich userow i clan

       TODO Załadować dana il. użytkowników do topki (np. top 50)
       TODO VaultUnlocked hook żeby robić upgrade size klanu -- done?

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
                PointsModule(),
                DataSourceModule(3), //TODO
                ManagersModule(),
                ListenersModule(),
                PreloadModule(),
                TasksModule(),
                CommandsModule(),
                VersionCheckModule()
            ),
            this.logger
        )

        runBlocking {
            initializer.enableAll(ctx)
        }

        this.logger.info("Plugin has been 'successfully loaded' in '${System.currentTimeMillis() - startTime}ms!'")
    }

    override fun onDisable() {
        runBlocking {
            if (this@ClansPlugin::initializer.isInitialized && this@ClansPlugin::ctx.isInitialized)
                initializer.disableAll(ctx)

        }
        this.logger.shutdown()
        this.job.cancel()

        super.getLogger().info("Plugin has been successfully disabled!")
    }
}