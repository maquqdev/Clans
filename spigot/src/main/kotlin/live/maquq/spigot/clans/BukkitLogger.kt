package live.maquq.spigot.clans

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    private val logDateFormat = SimpleDateFormat("HH:mm")
    private val fileDateFormat = SimpleDateFormat("yyyy/MM/dd") //enterprise date format (only valid ofc)

    private val logFolder = File(plugin.dataFolder, "logs")
    private var currentLogFile: File? = null

    init {
        if (!this.logFolder.exists())
            this.logFolder.mkdirs()

        this.startFileWriter()
    }

    fun info(message: String) {
        this.log(Level.INFO, message)
    }

    fun warn(message: String) {
        this.log(Level.WARNING, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        this.log(Level.SEVERE, message, throwable)
    }

    fun debug(message: String) {
        if (this.debugMode) this.log(Level.INFO, "<gray>[DEBUG] $message")
    }

    private fun log(level: Level, message: String, throwable: Throwable? = null) {
        val consoleComponent = this.formatForConsole(level, message)
        val fileMessage = this.formatForFile(level, message)

        val legacyMessage = this.legacySerializer.serialize(consoleComponent)

        this.plugin.logger.log(level, legacyMessage, throwable)

        this.scope.launch {
            this.runCatching {
                logChannel.send(fileMessage)
                throwable.let { if (it != null) logChannel.send(it.stackTraceToString()) }
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

    private fun formatForConsole(level: Level, message: String): Component {
        val messageComponent = this.miniMessage.deserialize(message)
        val color = when (level) {
            Level.WARNING -> NamedTextColor.YELLOW
            Level.SEVERE -> NamedTextColor.RED
            else -> NamedTextColor.AQUA
        }
        return messageComponent.style(messageComponent.style().colorIfAbsent(color))
    }

    private fun formatForFile(level: Level, message: String): String {
        val timestamp = this.logDateFormat.format(Date())
        val levelName = level.name.padEnd(7) //tuff kot
        val parsedComponent = this.miniMessage.deserialize(message)
        val cleanMessage = PlainTextComponentSerializer.plainText().serialize(parsedComponent)

        return "[$timestamp] [$levelName] $cleanMessage"
    }

    fun shutdown() {
        this.logChannel.close()
        this.job.cancel()
    }
}
