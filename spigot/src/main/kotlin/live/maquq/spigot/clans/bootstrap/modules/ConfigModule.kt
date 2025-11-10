package live.maquq.spigot.clans.bootstrap.modules

import com.bruhdows.minitext.MiniText
import com.bruhdows.minitext.formatter.FormatterType
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.configuration.Config
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import java.io.File

class ConfigModule : PluginModule {
    override val name: String = "Config"

    override suspend fun enable(ctx: PluginContext) {
        ctx.miniText = MiniText.builder()
            .enableFormatter(
                FormatterType.LEGACY,
                FormatterType.NAMED_COLORS,
                FormatterType.HEX,
                FormatterType.NEW_LINES,
                FormatterType.DECORATIONS,
                FormatterType.RESET
            )
            .build()

        val logger: BukkitLogger = ctx.logger
        val dataFolder = ctx.plugin.dataFolder

        ctx.mainConfig = Config(
            PluginConfiguration::class.java,
            File(dataFolder, "config.json"),
            logger
        )

        ctx.guiConfig = Config(
            GuiConfiguration::class.java,
            File(dataFolder, "guiConfiguration.json"),
            logger
        )
    }

    override suspend fun disable(ctx: PluginContext) {
        runCatching { ctx.mainConfig.shutdown() }
    }
}