package live.maquq.spigot.clans.manager


import live.maquq.api.DataSource
import live.maquq.api.clan.Clan
import live.maquq.api.clan.ClanRole
import live.maquq.api.User
import live.maquq.spigot.clans.BukkitLogger

class ClanManager(
    private val dataSource: DataSource,
    private val logger: BukkitLogger
) {

    suspend fun getClan(tag: String): Clan? {
        this.logger.debug("Pobieranie klanu o tagu: $tag")
        val clan = this.dataSource.loadClan(tag) ?: return null

        clan.init { ownerUuid ->
            if (ownerUuid == null) null else this.dataSource.loadUser(ownerUuid)
        }

        return clan
    }

    suspend fun saveClan(clan: Clan) {
        this.logger.debug("Zapisywanie klanu: ${clan.tag}")
        this.dataSource.saveClan(clan)
    }

    fun createNewClan(tag: String, owner: User): Clan {
        this.logger.info("Tworzenie nowego klanu '$tag' przez gracza ${owner.uuid}")
        return Clan(
            tag = tag,
            ownerUuid = owner.uuid,
            members = mutableMapOf(owner.uuid to ClanRole.LEADER)
        )
    }

    suspend fun deleteClan(clan: Clan) {
        this.logger.warn("Usuwanie klanu: ${clan.tag}")
        for (memberUuid in clan.members.keys) {
            val user = dataSource.loadUser(memberUuid)
            if (user != null) {
                val updatedUser = user.copy(clanTag = null)
                dataSource.saveUser(updatedUser)
            }
        }
        this.dataSource.deleteClan(clan.tag)
    }

}