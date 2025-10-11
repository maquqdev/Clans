package live.maquq.spigot.clans.listener

import com.bruhdows.minitext.MiniText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.configuration.impl.PlayerDeathConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.points.PointsManager
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class PlayerDeathListener(
    private val scope: CoroutineScope,
    private val miniText: MiniText,
    private val playerDeathConfiguration: PlayerDeathConfiguration,

    private val pointsManager: PointsManager,
    private val userManager: UserManager
) : Listener {

    @EventHandler
    fun handlePlayerDeath(event: PlayerDeathEvent) {
        val victim = event.player
        val killer = event.player.killer ?: return

        this.scope.launch {
            val killerUser = userManager.getUser(killer.uniqueId)
            killerUser.kills++
            val victimUser = userManager.getUser(victim.uniqueId)
            victimUser.deaths++

            val calculatedPointsPair = pointsManager.removePointsFromPlayer(
                killerUser,
                victimUser
            )

            val mainTitleTranslated = miniText.deserialize(playerDeathConfiguration.title).component()
            val subtitleTranslated = miniText.deserialize(playerDeathConfiguration.subtitle.replace(
                "[POINTS]", calculatedPointsPair.first.toString())
            ).component()

            val victimTitleTranslated = miniText.deserialize(playerDeathConfiguration.victimTitle).component()
            val victimSubtitleTranslated = miniText.deserialize(playerDeathConfiguration.victimSubtitle.replace(
                "[POINTS]", calculatedPointsPair.second.toString()
            )).component()

            Title.title(
                mainTitleTranslated,
                subtitleTranslated
            ).let {
                killer.showTitle(it)
            }

            Title.title(
                victimTitleTranslated,
                victimSubtitleTranslated
            ).let {
                victim.showTitle(it)
            }

            miniText.deserialize(
                playerDeathConfiguration.broadcast
                    .replace("[KILLER]", killer.name)
                    .replace("[ADDED-POINTS]", calculatedPointsPair.first.toString())
                    .replace("[VICTIM]", victim.name)
                    .replace("[REMOVED-POINTS]", calculatedPointsPair.second.toString())
            ).component().let {
                Bukkit.broadcast(it)
            }
        }
    }
}