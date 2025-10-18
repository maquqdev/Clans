package live.maquq.api.events.user

import live.maquq.api.common.ClanQuitCause
import live.maquq.api.user.User
import live.maquq.api.user.clan.Clan
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class UserQuitClanEvent(
    val user: User,
    val clan: Clan,
    val clanQuitCause: ClanQuitCause
) : Event(true) {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}