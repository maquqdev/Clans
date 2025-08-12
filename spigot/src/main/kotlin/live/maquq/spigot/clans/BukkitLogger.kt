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
    private val fileDateFormat = SimpleDateFormat("yyyy/MM/dd")

    private val logFolder = File(plugin.dataFolder, "logs")
    private var currentLogFile: File? = null

    init {
        if (!logFolder.exists()) {
            logFolder.mkdirs()
        }
        startFileWriter()
    }

    fun info(message: String) {
        log(Level.INFO, message)
    }

    fun warn(message: String) {
        log(Level.WARNING, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        log(Level.SEVERE, message, throwable)
    }

    fun debug(message: String) {
        if (debugMode) log(Level.INFO, "<gray>[DEBUG] $message")
    }

    fun shutdown() {
        logChannel.close(); job.cancel()
    }

    private fun log(level: Level, message: String, throwable: Throwable? = null) {
        val consoleComponent = formatForConsole(level, message)
        val fileMessage = formatForFile(level, message)

        val legacyMessage = this.legacySerializer.serialize(consoleComponent)

        this.plugin.logger.log(level, legacyMessage, throwable)

        scope.launch {
            try {
                logChannel.send(fileMessage)
                if (throwable != null) {
                    logChannel.send(throwable.stackTraceToString())
                }
            } catch (ignored: ClosedSendChannelException) {
            }
        }
    }

    private fun startFileWriter() = scope.launch {
        for (message in logChannel) {
            try {
                val file = getAndPrepareTodaysLogFile()
                file.appendText("$message\n")
            } catch (e: Exception) {
                plugin.logger.log(
                    Level.SEVERE,
                    "Krytyczny błąd zapisu do pliku logu! Zapis do pliku zostaje wyłączony.",
                    e
                )
                logChannel.close()
            }
        }
    }

    private fun getAndPrepareTodaysLogFile(): File {
        val today = fileDateFormat.format(Date())
        val currentFileName = "$today.log"
        if (currentLogFile?.name == currentFileName) return currentLogFile!!
        val newLogFile = File(logFolder, currentFileName)
        if (!newLogFile.parentFile.exists()) newLogFile.parentFile.mkdirs()
        return newLogFile.also { currentLogFile = it }
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
        val timestamp = logDateFormat.format(Date())
        val levelName = level.name.padEnd(7)
        val parsedComponent = this.miniMessage.deserialize(message)
        val cleanMessage = PlainTextComponentSerializer.plainText().serialize(parsedComponent)
        return "[$timestamp] [$levelName] $cleanMessage"
    }
}