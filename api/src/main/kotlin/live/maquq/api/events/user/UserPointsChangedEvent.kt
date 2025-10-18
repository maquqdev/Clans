package live.maquq.api.events.user

import live.maquq.api.common.DeathCause
import live.maquq.api.user.User
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class UserPointsChangedEvent(
    val user: User,
    val points: Int,
    val deathCause: DeathCause
) : Event(true) {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}