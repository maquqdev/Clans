package live.maquq.spigot.clans.manager.module

import java.util.*

data class ClanInvite(
    val clanTag: String,
    val invitedByUuid: UUID,
    val timestamp: Long = System.currentTimeMillis()
)