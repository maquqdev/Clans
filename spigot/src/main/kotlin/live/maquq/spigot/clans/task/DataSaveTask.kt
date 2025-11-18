package live.maquq.spigot.clans.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager

class DataSaveTask(
    private val scope: CoroutineScope,
    private val userManager: UserManager,
    private val clanManager: ClanManager,

    private val logger: BukkitLogger
) : Runnable {

    override fun run() {
        this.scope.launch {
            userManager.all().forEach { user ->
                userManager.saveUser(user)
            }

            clanManager.all().forEach { clan ->
                clanManager.saveClan(clan)
            }

            logger.debug("Saved every user and clan to database!")
        }
    }
}