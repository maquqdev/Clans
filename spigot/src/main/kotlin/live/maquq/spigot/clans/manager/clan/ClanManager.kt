package live.maquq.spigot.clans.manager.clan


import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import live.maquq.api.data.DataSource
import live.maquq.api.events.clan.ClanCreateEvent
import live.maquq.api.events.clan.ClanDeleteEvent
import live.maquq.api.events.user.UserJoinClanEvent
import live.maquq.api.user.clan.Clan
import live.maquq.api.common.ClanRole
import live.maquq.api.user.User
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.module.ClanInvite
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ClanManager(
    private val dataSource: DataSource,
    private val userManager: UserManager,
    private val mainConfig: PluginConfiguration,
    private val logger: BukkitLogger
) {

    private val clanCache: MutableMap<String, Clan> = ConcurrentHashMap()
    private val topClansCache: MutableMap<String, ClanStats> = ConcurrentHashMap()
    private val pendingInvites: MutableMap<UUID, ClanInvite> = ConcurrentHashMap()
    private val requestDelete: MutableMap<UUID, Clan> = ConcurrentHashMap()

    suspend fun getClan(tag: String): Clan? {
        val cachedClan = this.clanCache[tag]
        if (cachedClan != null) {
            this.logger.debug("Loaded '$tag' from cache.")
            return cachedClan
        }

        this.logger.debug("Cannot find '$tag' in cache, loading from database...")
        val clanFromDb = this.dataSource.loadClan(tag) ?: return null

        this.clanCache[tag] = clanFromDb
        this.logger.debug("Saved clan '$tag' in cache.")

        return clanFromDb
    }

    fun getCachedClan(tag: String): Clan? {
        return this.clanCache[tag]
    }

    suspend fun saveClan(clan: Clan) {
        this.logger.debug("Saving '${clan.tag}' to database and cache...")
        this.dataSource.saveClan(clan)
        this.clanCache[clan.tag] = clan
        this.logger.debug("Saved '${clan.tag}' to database and cache!")
    }

    suspend fun deleteClan(clan: Clan) {
        this.logger.debug("Deleting clan '${clan.tag}' from database and cache...")

        for (memberUuid in clan.members.keys) {
            val user = this.userManager.getUser(memberUuid)
            val updatedUser = user.copy(clanTag = null)

            this.userManager.saveUser(updatedUser)
        }

        this.dataSource.deleteClan(clan.tag)
        this.clanCache.remove(clan.tag)

        Bukkit.getPluginManager().callEvent(ClanDeleteEvent(clan))

        this.logger.debug("Deleted clan ${clan.tag} and saved user!")
    }

    suspend fun preloadAllClansToCache() {
        this.logger.debug("Loading 'every clan' to cache...")
        val allClans = this.dataSource.getAllClans()
        allClans.forEach { clan ->
            this.clanCache[clan.tag] = clan
        }
        this.logger.debug("Loaded '${allClans.size}' clans to cache.")
    }

    suspend fun updateTopClansCache() {
        this.logger.debug("Updating top clans cache...")
        this.topClansCache.clear()
        val allClans = this.dataSource.getAllClans()
        for (clan in allClans) {
            val stats = calculateClanStats(clan)
            this.topClansCache[clan.tag] = stats
        }
        this.logger.debug("Updated top clans cache with ${this.topClansCache.size} clans")
    }

    fun createNewClan(tag: String, owner: User): Clan {
        this.logger.debug("Creating clan '$tag' to owner ${owner.uuid}")

        val clan = Clan(
            tag = tag,
            ownerUuid = owner.uuid,
            members = mutableMapOf(owner.uuid to ClanRole.LEADER),
            maxSize = this.mainConfig.clanSettings.defaultSize,
            pointsMultiplier = this.mainConfig.clanSettings.defaultPointsMultiplier
        )

        Bukkit.getPluginManager().callEvent(ClanCreateEvent(clan))
        return clan
    }

    fun deleteRequest(
        user: User,
        clan: Clan
    ): Boolean {
        val requestedClan = this.requestDelete[user.uuid]
        if (requestedClan != null) return true

        this.requestDelete[user.uuid] = clan
        return false
    }

    fun invitePlayer(
        inviter: User,
        target: User,
        clan: Clan
    ) {
        val invite = ClanInvite(
            clan.tag,
            inviter.uuid
        )
        this.pendingInvites[target.uuid] = invite
        this.logger.debug("Player ${inviter.uuid} invited ${target.uuid} to clan ${clan.tag}")
    }

    suspend fun acceptInvite(user: User): Boolean {
        val invite = this.pendingInvites[user.uuid] ?: return false

        val inviteAgeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - invite.timestamp)
        if (inviteAgeSeconds > this.mainConfig.clanSettings.timeToTimeoutInvite) {
            this.pendingInvites.remove(user.uuid)
            return false
        }

        val clan = this.getClan(invite.clanTag) ?: return false

        clan.members[user.uuid] = ClanRole.MEMBER
        this.saveClan(clan)

        val updatedUser = user.copy(clanTag = clan.tag)
        this.userManager.saveUser(updatedUser)

        this.pendingInvites.remove(user.uuid)

        Bukkit.getPluginManager().callEvent(UserJoinClanEvent(user, clan))

        this.logger.debug("Player '${user.uuid}' joined to clan ${clan.tag}")
        return true
    }

    suspend fun averagePoints(clan: Clan, userManager: UserManager): Int {
        if (clan.members.isEmpty())
            return this.mainConfig.clanSettings.defaultPoints

        val totalPoints = coroutineScope {
            val usersPoints = clan.members.map { member ->
                async {
                    userManager.getUser(member.key).points
                }
            }

            usersPoints.awaitAll().sum()
        }

        return totalPoints / clan.members.size
    }

    fun all(): List<Clan> {
        return this.clanCache.values.toList()
    }

    fun getCachedTopClansByStat(stat: String, limit: Int = 50): List<Clan> {
        return this.topClansCache.entries
            .sortedByDescending { (_, stats) -> stats.getStatValue(stat) }
            .take(limit)
            .mapNotNull { (tag, _) -> this.clanCache[tag] }
    }

    suspend fun calculateClanStats(clan: Clan): ClanStats {
        var totalKills = 0
        var totalDeaths = 0
        var totalAssists = 0
        var totalPoints = 0

        for (memberUuid in clan.members.keys) {
            val user = this.userManager.getUser(memberUuid)
            totalKills += user.kills
            totalDeaths += user.deaths
            totalAssists += user.assists
            totalPoints += user.points
        }

        clan.members.size.let { totalMembers ->
            totalPoints /= totalMembers
            totalKills /= totalMembers
            totalDeaths /= totalMembers
            totalAssists /= totalMembers
        }

        return ClanStats(totalKills, totalDeaths, totalAssists, totalPoints)
    }

    data class ClanStats(
        val kills: Int,
        val deaths: Int,
        val assists: Int,
        val points: Int
    ) {
        fun getStatValue(stat: String): Int = when (stat) {
            "KILLS" -> kills
            "DEATHS" -> deaths
            "ASSISTS" -> assists
            "POINTS" -> points
            else -> 0
        }
    }
}