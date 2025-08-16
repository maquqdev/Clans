package live.maquq.spigot.clans.manager


import live.maquq.api.DataSource
import live.maquq.api.clan.Clan
import live.maquq.api.clan.ClanRole
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.module.ClanInvite
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ClanManager(
    private val dataSource: DataSource,
    private val mainConfig: PluginConfiguration,
    private val logger: BukkitLogger
) {

    private val clanCache: MutableMap<String, Clan> = ConcurrentHashMap()

    private val pendingInvites: MutableMap<UUID, ClanInvite> = ConcurrentHashMap()

    suspend fun getClan(tag: String): Clan? {
        val cachedClan = this.clanCache[tag]
        if (cachedClan != null) {
            this.logger.debug("Loaded '$tag' from cache.")
            return cachedClan
        }

        this.logger.debug("Cannot find '$tag' in cache, loading from database...")
        val clanFromDb = this.dataSource.loadClan(tag) ?: return null

        clanFromDb.init { ownerUuid ->
            if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid)
        }

        this.clanCache[tag] = clanFromDb
        this.logger.debug("Saved clan '$tag' in cache.")

        return clanFromDb
    }

    suspend fun saveClan(clan: Clan) {
        this.logger.debug("Saving ${clan.tag} to database and cache...")
        this.dataSource.saveClan(clan)
        this.clanCache[clan.tag] = clan
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
    }

    suspend fun preloadAllClansToCache() {
        this.logger.debug("Loading every clan to cache...")
        val allClans = this.dataSource.getAllClans()
        allClans.forEach { clan ->
            clan.init { ownerUuid -> if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid) }
            this.clanCache[clan.tag] = clan
        }
        this.logger.debug("Loaded ${allClans.size} clans to cache.")
    }

    fun createNewClan(tag: String, owner: User): Clan {
        this.logger.debug("Creating clan '$tag' to owner ${owner.uuid}")
        return Clan(
            tag = tag,
            ownerUuid = owner.uuid,
            members = mutableMapOf(owner.uuid to ClanRole.LEADER)
        )
    }

    suspend fun invitePlayer(inviter: User, target: User, clan: Clan) {
        // TODO: Validation if player has permission to invite the player

//        if (target.clanTag != null) {
//            return
//        }

        val invite = ClanInvite(clan.tag, inviter.uuid)
        this.pendingInvites[target.uuid] = invite
        this.logger.info("Gracz ${inviter.uuid} zaprosił ${target.uuid} do klanu ${clan.tag}")
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
        this.dataSource.saveUser(updatedUser)

        this.pendingInvites.remove(joiningUser.uuid)

        this.logger.info("Gracz ${joiningUser.uuid} dołączył do klanu ${clan.tag}")
        return true
    }

    //irrelevant tbh
//    fun denyInvite(playerUuid: UUID): Boolean {
//        return this.pendingInvites.remove(playerUuid) != null
//    }


    fun getAllClans(): List<Clan> {
        return this.clanCache.values.toList()
    }
}