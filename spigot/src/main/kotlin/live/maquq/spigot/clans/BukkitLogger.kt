package live.maquq.spigot.clans

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

class BukkitLogger(
    private val plugin: JavaPlugin,
    private val debugMode: Boolean
) {

    private val job = SupervisorJob()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + job)
    private val logChannel = Channel<String>(Channel.UNLIMITED)

    private val miniMessage = MiniMessage.miniMessage()

    private val logDateFormat = SimpleDateFormat("HH:mm")
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd")

    private val logFolder = File(plugin.dataFolder, "logs")
    private var currentLogFile: File? = null

    init {
        if (!this.logFolder.exists())
            this.logFolder.mkdirs()

        this.startFileWriter()
    }

    fun info(message: String) {
        this.log(Level.INFO, "<gray>[<yellow>INFO<gray>]<reset>   <white>${processQuotedText(message)}")
    }

    fun warn(message: String) {
        this.log(Level.WARNING, "<gray>[<yellow>WARN<gray>]<reset>   <white>${processQuotedText(message)}")
    }

    fun error(message: String, throwable: Throwable? = null) {
        this.log(Level.SEVERE, "<gray>[<red>ERROR<gray>]<reset>  <white>${processQuotedText(message)}", throwable)
    }

    fun debug(message: String) {
        if (this.debugMode) this.log(Level.INFO, "<gray>[<blue>DEBUG<gray>]<reset>  <white>${processQuotedText(message)}")
    }

    private fun processQuotedText(message: String): String {
        return message.replace(Regex("'([^']+)'")) { matchResult ->
            "<blue>${matchResult.groupValues[1]}<white>"
        }
    }

    private fun log(level: Level, message: String, throwable: Throwable? = null) {
        val parsedComponent = this.miniMessage.deserialize(message)
        val consoleComponent = this.formatForConsole(level, parsedComponent)
        val fileMessage = this.formatForFile(level, parsedComponent)

        val ansiMessage = this.componentToAnsi(consoleComponent)

        this.plugin.logger.log(level, ansiMessage, throwable)

        this.scope.launch {
            this.runCatching {
                logChannel.send(fileMessage)
                throwable?.let { logChannel.send(it.stackTraceToString()) }
            }
        }
    }

    private fun startFileWriter() = this.scope.launch {
        for (message in logChannel) {
            try {
                val file = getAndPrepareTodayLogFile()
                file.appendText("$message\n")
            } catch (exception: Exception) {
                plugin.logger.log(
                    Level.SEVERE,
                    "Critical error writing to log file! Logging to file is disabled.",
                    exception
                )
                logChannel.close()
            }
        }
    }

    private fun getAndPrepareTodayLogFile(): File {
        val today = this.fileDateFormat.format(Date())
        val currentFileName = "$today.log"
        if (this.currentLogFile?.name == currentFileName) return this.currentLogFile!!

        val newLogFile = File(this.logFolder, currentFileName)
        if (!newLogFile.parentFile.exists()) newLogFile.parentFile.mkdirs()

        return newLogFile.also { this.currentLogFile = it }
    }

    private fun formatForConsole(level: Level, component: Component): Component {
        val color = when (level) {
            Level.WARNING -> NamedTextColor.YELLOW
            Level.SEVERE -> NamedTextColor.RED
            else -> NamedTextColor.AQUA
        }
        return component.colorIfAbsent(color)
    }

    private fun formatForFile(level: Level, component: Component): String {
        val timestamp = this.logDateFormat.format(Date())
        val levelName = level.name.padEnd(7)
        val cleanMessage = PlainTextComponentSerializer.plainText().serialize(component)

        return "[$timestamp] [$levelName] $cleanMessage"
    }

    private fun componentToAnsi(component: Component): String {
        val builder = StringBuilder()
        traverseComponent(component, builder, State())
        if (builder.isNotEmpty())
            builder.append("\u001B[0m")

        return builder.toString()
    }

    private data class State(
        var currentColor: TextColor? = null,
        val activeDecorations: MutableSet<TextDecoration> = mutableSetOf()
    )

    private fun traverseComponent(component: Component, builder: StringBuilder, state: State) {
        val style = component.style()
        val newColor = style.color()
        val newDecorations = TextDecoration.values().filter {
            style.decoration(it) == TextDecoration.State.TRUE
        }.toSet()

        if (newColor != null && newColor != state.currentColor) {
            builder.append(textColorToAnsi(newColor))
            state.currentColor = newColor
        }

        val decorationsToAdd = newDecorations - state.activeDecorations
        decorationsToAdd.forEach { decoration ->
            builder.append(decorationToAnsi(decoration))
            state.activeDecorations.add(decoration)
        }

        if (component is net.kyori.adventure.text.TextComponent) {
            val content = component.content()
            if (content.isNotEmpty()) {
                builder.append(content)
            }
        }

        component.children().forEach { child ->
            traverseComponent(child, builder, state)
        }
    }

    private fun decorationToAnsi(decoration: TextDecoration): String {
        return when (decoration) {
            TextDecoration.BOLD -> "\u001B[1m"
            TextDecoration.ITALIC -> "\u001B[3m"
            TextDecoration.UNDERLINED -> "\u001B[4m"
            TextDecoration.STRIKETHROUGH -> "\u001B[9m"
            TextDecoration.OBFUSCATED -> ""
        }
    }

    private fun textColorToAnsi(color: TextColor): String {
        return when (color) {
            NamedTextColor.BLACK -> "\u001B[30m"
            NamedTextColor.DARK_BLUE -> "\u001B[34m"
            NamedTextColor.DARK_GREEN -> "\u001B[32m"
            NamedTextColor.DARK_AQUA -> "\u001B[36m"
            NamedTextColor.DARK_RED -> "\u001B[31m"
            NamedTextColor.DARK_PURPLE -> "\u001B[35m"
            NamedTextColor.GOLD -> "\u001B[33m"
            NamedTextColor.GRAY -> "\u001B[37m"
            NamedTextColor.DARK_GRAY -> "\u001B[90m"
            NamedTextColor.BLUE -> "\u001B[94m"
            NamedTextColor.GREEN -> "\u001B[92m"
            NamedTextColor.AQUA -> "\u001B[96m"
            NamedTextColor.RED -> "\u001B[91m"
            NamedTextColor.LIGHT_PURPLE -> "\u001B[95m"
            NamedTextColor.YELLOW -> "\u001B[93m"
            NamedTextColor.WHITE -> "\u001B[97m"
            else -> {
                val rgb = color.value()
                approximateRgbToAnsi(rgb)
            }
        }
    }

    private fun approximateRgbToAnsi(rgb: Int): String {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF

        return when {
            r > 200 && g < 100 && b < 100 -> "\u001B[91m"
            r < 100 && g > 200 && b < 100 -> "\u001B[92m"
            r < 100 && g < 100 && b > 200 -> "\u001B[94m"
            r > 200 && g > 200 && b < 100 -> "\u001B[93m"
            r > 200 && g < 100 && b > 200 -> "\u001B[95m"
            r < 100 && g > 200 && b > 200 -> "\u001B[96m"
            r > 150 && g > 150 && b > 150 -> "\u001B[97m"
            r < 100 && g < 100 && b < 100 -> "\u001B[90m"
            else -> "\u001B[37m"
        }
    }

    fun shutdown() {
        this.logChannel.close()
        this.job.cancel()
    }
}