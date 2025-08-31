package live.maquq.spigot.clans

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin
import java.net.HttpURLConnection
import java.net.URL

class VersionChecker(
    private val plugin: JavaPlugin,
    private val logger: BukkitLogger,
    private val scope: CoroutineScope
) {

    private val versionUrl = URL("https://raw.githubusercontent.com/maquqdev/Clans/main/version.txt")

    fun check() {
        scope.launch {
            try {
                val connection = versionUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                connection.inputStream.bufferedReader().use { reader ->
                    val remoteVersion = reader.readLine()
                    val changelog = reader.readLine()

                    if (remoteVersion == null || changelog == null) {
                        logger.warn("Could not parse version file from GitHub.")
                        return@launch
                    }

                    val localVersion = plugin.description.version
                    if (remoteVersion != localVersion) {
                        logUpdateMessage(localVersion, remoteVersion, changelog)
                    } else {
                        logger.info("You are using the latest version of Clans ($localVersion).")
                    }
                }
            } catch (e: Exception) {
                logger.error("Could not check for a new version of Clans.", e)
            }
        }
    }

    private fun logUpdateMessage(local: String, remote: String, changelog: String) {
        logger.warn(" ")
        logger.warn("<gold> /\\_/\\  <gray>Clans - Dostępna nowa aktualizacja!</gold>")
        logger.warn("<gold>( o.o )</gold> Twoja wersja (<red>$local</red>) jest przestarzała!")
        logger.warn("<gold> > ^ < </gold> Nowa wersja: <green>$remote</green>")
        logger.info("         <aqua>Zmiany: $changelog")
        logger.warn("<gold>        Pobierz ją z: https://github.com/maquqdev/Clans/releases/latest")
        logger.warn(" ")
    }
}