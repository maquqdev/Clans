package live.maquq.spigot.clans.manager.clan


import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import live.maquq.api.DataSource
import live.maquq.api.clan.Clan
import live.maquq.api.clan.ClanRole
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.module.ClanInvite
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

    suspend fun saveClan(clan: Clan) {
        this.logger.debug("Saving ${clan.tag} to database and cache...")
        this.dataSource.saveClan(clan)
        this.clanCache[clan.tag] = clan
        this.logger.debug("Saved ${clan.tag} to database and cache!")
    }

    suspend fun deleteClan(clan: Clan) {
        this.logger.debug("Deleting clan ${clan.tag} from database and cache...")

        for (memberUuid in clan.members.keys) {
            val user = dataSource.loadUser(memberUuid)
            if (user != null) {
                val updatedUser = user.copy(clanTag = null)
                dataSource.saveUser(updatedUser)
            }
        }

        this.dataSource.deleteClan(clan.tag)
        this.clanCache.remove(clan.tag)
        this.logger.debug("Deleted clan ${clan.tag} and saved user!")
    }

    suspend fun preloadAllClansToCache() {
        this.logger.debug("Loading every clan to cache...")
        val allClans = this.dataSource.getAllClans()
        allClans.forEach { clan ->
            this.clanCache[clan.tag] = clan
        }
        this.logger.debug("Loaded ${allClans.size} clans to cache.")
    }

    fun createNewClan(tag: String, owner: User): Clan {
        this.logger.debug("Creating clan '$tag' to owner ${owner.uuid}")
        return Clan(
            tag = tag,
            ownerUuid = owner.uuid,
            members = mutableMapOf(owner.uuid to ClanRole.LEADER),
            maxSize = this.mainConfig.clanSettings.defaultSize,
            pointsMultiplier = 1.0
        )
    }

    fun deleteRequest(
        user: User,
        clan: Clan
    ): Boolean {
        val requestedClan = this.requestDelete[user.uuid]
        if(requestedClan != null) return true

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

    suspend fun acceptInvite(joiningUser: User): Boolean {
        val invite = this.pendingInvites[joiningUser.uuid] ?: return false

        val inviteAgeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - invite.timestamp)
        if (inviteAgeSeconds > this.mainConfig.clanSettings.timeToTimeoutInvite) {
            this.pendingInvites.remove(joiningUser.uuid)
            return false
        }

        val clan = this.getClan(invite.clanTag) ?: return false

        clan.members[joiningUser.uuid] = ClanRole.MEMBER
        this.saveClan(clan)

        val updatedUser = joiningUser.copy(clanTag = clan.tag)
        this.userManager.saveUser(updatedUser)

        this.pendingInvites.remove(joiningUser.uuid)

        this.logger.debug("Player ${joiningUser.uuid} joined to clan ${clan.tag}")
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
}