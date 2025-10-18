package live.maquq.api.events.clan

import live.maquq.api.user.clan.Clan
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ClanUpgradeEvent(
    val clan: Clan
) : Event(true) {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}