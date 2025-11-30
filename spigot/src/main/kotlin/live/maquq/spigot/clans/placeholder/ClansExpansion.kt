package live.maquq.spigot.clans.placeholder

import kotlinx.coroutines.runBlocking
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class ClansExpansion(
    private val userManager: UserManager,
    private val clanManager: ClanManager
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "clans"

    override fun getAuthor(): String = "maquq"

    override fun getVersion(): String = "1.0"

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        val parts = params.split("_")
        if (parts.isEmpty()) return null

        return when (parts[0]) {
            "user" -> handleUserPlaceholders(player, parts)
            "users" -> handleUserLeaderboardPlaceholders(parts)
            "clan" -> handleClanPlaceholders(player, parts)
            "clans" -> handleClanLeaderboardPlaceholders(parts)
            else -> null
        }
    }

    private fun handleUserPlaceholders(player: OfflinePlayer, parts: List<String>): String? {
        if (parts.size < 2) return null

        val user = runBlocking { userManager.getUser(player.uniqueId) }

        return when (parts[1].uppercase()) {
            "KILLS" -> user.kills.toString()
            "DEATHS" -> user.deaths.toString()
            "ASSISTS" -> user.assists.toString()
            "POINTS" -> user.points.toString()
            else -> null
        }
    }

    private fun handleUserLeaderboardPlaceholders(parts: List<String>): String {
        if (parts.size < 3) return "0"

        val position = parts[1].toIntOrNull() ?: return "0"
        val stat = parts[2].uppercase()

        val topUsers = userManager.getCachedTopUsers(100)

        if (position < 1 || position > topUsers.size) return "0"

        val user = topUsers[position - 1]

        return when (stat) {
            "KILLS" -> user.kills.toString()
            "DEATHS" -> user.deaths.toString()
            "ASSISTS" -> user.assists.toString()
            "POINTS" -> user.points.toString()
            else -> "0"
        }
    }

    private fun handleClanPlaceholders(player: OfflinePlayer, parts: List<String>): String {
        if (parts.size < 2) return "0"

        val user = runBlocking { userManager.getUser(player.uniqueId) }
        val clanTag = user.clanTag ?: return "0"

        val clan = runBlocking { clanManager.getClan(clanTag) } ?: return "0"

        val clanStats = runBlocking { clanManager.calculateClanStats(clan) }

        return when (parts[1].uppercase()) {
            "KILLS" -> clanStats.kills.toString()
            "DEATHS" -> clanStats.deaths.toString()
            "ASSISTS" -> clanStats.assists.toString()
            "POINTS" -> clanStats.points.toString()
            else -> "0"
        }
    }

    private fun handleClanLeaderboardPlaceholders(parts: List<String>): String {
        if (parts.size < 3) return "0"

        val position = parts[1].toIntOrNull() ?: return "0"
        val stat = parts[2].uppercase()

        val topClans = clanManager.getCachedTopClansByStat(stat, 100)
        if (position < 1 || position > topClans.size) return "0"

        val clan = topClans[position - 1]
        val clanStats = runBlocking { clanManager.calculateClanStats(clan) }

        return when (stat) {
            "KILLS" -> clanStats.kills.toString()
            "DEATHS" -> clanStats.deaths.toString()
            "ASSISTS" -> clanStats.assists.toString()
            "POINTS" -> clanStats.points.toString()
            else -> "0"
        }
    }

}