package live.maquq.spigot.clans.bootstrap.modules

import live.maquq.api.data.DataSource
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.configuration.impl.StorageType
import live.maquq.storage.impl.FlatDataSource
import live.maquq.storage.impl.MongoDataSource
import live.maquq.storage.impl.MySqlDataSource

class DataSourceModule() : PluginModule {
    override val name: String = "Database"

    override suspend fun enable(ctx: PluginContext) {
        val config: PluginConfiguration = ctx.mainConfig.get
        ctx.dataSource = when (config.storage) {
            StorageType.FLAT -> FlatDataSource(ctx.plugin.dataFolder)
            StorageType.MYSQL -> MySqlDataSource(
                mapOf(
                    "host" to config.mysql.host,
                    "port" to config.mysql.port,
                    "database" to config.mysql.database,
                    "username" to config.mysql.username,
                    "password" to config.mysql.password
                ),
                ctx.mainConfig.get.clanSettings.defaultSize
            )
            StorageType.MONGODB -> MongoDataSource(config.mongo.connectionString)
        }

        runCatching { ctx.dataSource.connect() }
            .onSuccess {
                ctx.logger.info("Successfully connected to database! '(${config.storage})'")
            }.onFailure { ex ->
                ctx.logger.error("Cannot connect to database, check configuration please!", ex)
                throw ex
            }
    }

    override suspend fun disable(ctx: PluginContext) {
        ctx.userManager.all().forEach { user ->
            ctx.userManager.saveUser(user)
        }

        ctx.clanManager.all().forEach { clan ->
            ctx.clanManager.saveClan(clan)
        }

        runCatching { ctx.dataSource.disconnect() }
    }
}