package live.maquq.spigot.clans.listener

import kotlinx.coroutines.CoroutineScope
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

class EntityDamageEntityListener(
    private val userManager: UserManager,
    private val clanManager: ClanManager,
    private val mainConfig: PluginConfiguration,
    private val scope: CoroutineScope
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun handleEntityDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return

        val attacker: Player? = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }

        val attackerPlayer = attacker ?: return
        if (attackerPlayer.uniqueId == victim.uniqueId) return

        val attackerUser = userManager.getCachedUser(attackerPlayer.uniqueId) ?: return
        val victimUser = userManager.getCachedUser(victim.uniqueId) ?: return

        val aClanTag = attackerUser.clanTag
        val vClanTag = victimUser.clanTag
        if (aClanTag == null || vClanTag == null || aClanTag != vClanTag)
            return

        val clan = clanManager.getCachedClan(aClanTag) ?: return

        if (!clan.pvpEnabled) {
            event.isCancelled = true
            return
        }

        if (event.damager is EnderCrystal
            && !mainConfig.clanSettings.crystalDamage) {
            event.damage = 0.0
        }
    }
}