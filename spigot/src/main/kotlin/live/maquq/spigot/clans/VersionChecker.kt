package live.maquq.spigot.clans

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.net.HttpURLConnection
import java.net.URL

class VersionChecker(
    private val plugin: JavaPlugin,
    private val logger: BukkitLogger,
    private val scope: CoroutineScope
) {

    private val versionUrl = URL("https://raw.githubusercontent.com/maquqdev/Clans/main/version.txt")
    private val apiUrl = URL("https://api.github.com/repos/maquqdev/Clans/commits/main")

    fun check() {
        this.scope.launch {
            try {
                val localVersion = plugin.description.version

                val remoteVersion = getRemoteVersion()
                if (remoteVersion == null) {
                    logger.warn("Could not fetch remote version from GitHub.")
                    return@launch
                }

                if (remoteVersion != localVersion) {
                    val commitInfo = getLatestCommitInfo()
                    if (commitInfo != null)
                        logUpdateMessage(localVersion, remoteVersion, commitInfo)
                     else
                        logUpdateMessage(localVersion, remoteVersion, CommitInfo("Unknown changes", "N/A"))
                } else {
                    logger.info("You are using the latest version of Clans ($localVersion).")
                }
            } catch (exception: Exception) {
                logger.error("Could not check for a new version of Clans.", exception)
            }
        }
    }

    private fun getRemoteVersion(): String? {
        return try {
            val connection = versionUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.inputStream.bufferedReader().use { reader ->
                reader.readLine()?.trim()
            }
        } catch (exception: Exception) {
            this.logger.error("Could not fetch version file.", exception)
            null
        }
    }

    private fun getLatestCommitInfo(): CommitInfo? {
        return try {
            val connection = apiUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseCommitResponse(response)
            } else {
                this.logger.warn("GitHub API returned ${connection.responseCode}: ${connection.responseMessage}")
                null
            }
        } catch (exception: Exception) {
            this.logger.error("Could not fetch commit information from GitHub API.", exception)
            null
        }
    }

    private fun parseCommitResponse(jsonResponse: String): CommitInfo? {
        return try {
            val parser = JSONParser()
            val jsonObject = parser.parse(jsonResponse) as JSONObject

            val commit = jsonObject["commit"] as JSONObject
            val message = commit["message"] as String
            val sha = jsonObject["sha"] as String

            val title = message.split("\n").first().trim()
            val shortSha = sha.take(7)

            CommitInfo(title, shortSha)
        } catch (exception: Exception) {
            this.logger.error("Could not parse commit information.", exception)
            null
        }
    }

    private fun logUpdateMessage(local: String, remote: String, commitInfo: CommitInfo) {
        this.logger.warn(" ")
        this.logger.warn("<blue> /\\_/\\   <blue>Clans - New update available!")
        this.logger.warn("<blue>( o.o )  <white>Your version: <blue>$local")
        this.logger.warn("<blue> > ^ <   <white>New version: <blue>$remote")
        this.logger.warn("      Changes:  <blue>${commitInfo.title}")
        this.logger.warn("      Download: <blue>https://github.com/maquqdev/Clans/releases/latest")
        this.logger.warn(" ")
    }

    private data class CommitInfo(
        val title: String,
        val shortSha: String
    )
}