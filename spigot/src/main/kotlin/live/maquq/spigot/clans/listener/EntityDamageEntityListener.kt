package live.maquq.spigot.clans.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class EntityDamageEntityListener(
    private val userManager: UserManager,
    private val clanManager: ClanManager,
    private val scope: CoroutineScope
) : Listener {

    @EventHandler
    fun handleEntityDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return

        val damagerPlayer: Player? = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> (damager.shooter as? Player)
            else -> null
        }

        val attacker = damagerPlayer ?: return
        if (attacker.uniqueId == victim.uniqueId) return

        this.scope.launch {
            val attackerUser = userManager.getUser(attacker.uniqueId)
            val victimUser = userManager.getUser(victim.uniqueId)

            val aClanTag = attackerUser.clanTag
            val vClanTag = victimUser.clanTag
            if (aClanTag == null ||
                vClanTag == null ||
                aClanTag != vClanTag) return@launch

            val clan = clanManager.getClan(aClanTag) ?: return@launch
            if (!clan.pvpEnabled)
                event.isCancelled = true
        }
    }
}
