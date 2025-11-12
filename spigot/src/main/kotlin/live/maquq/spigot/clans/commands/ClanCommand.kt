package live.maquq.spigot.clans.commands

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.maquq.api.common.ClanQuitCause
import live.maquq.api.events.user.UserQuitClanEvent
import live.maquq.api.user.clan.ClanRole
import live.maquq.spigot.clans.configuration.impl.GuiConfiguration
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.clans.menu.ClanMenu
import live.maquq.spigot.gui.manager.InventoryManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@Command(name = "klan")
class ClanCommand(
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration,
    private val scope: CoroutineScope,
    private val clanManager: ClanManager,
    private val userManager: UserManager,
    private val inventoryManager: InventoryManager,
    private val guiConfiguration: GuiConfiguration
) {

    @Execute(name = "stworz")
    fun createCommand(
        @Context player: Player,
        @Arg("tag") tag: String
    ) {
        scope.launch {
            if (clanManager.getClan(tag) != null) {
                miniText.deserialize(mainConfig.messages.clanAlreadyExists).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val user = userManager.getUser(player.uniqueId)

            if (user.clanTag != null) {
                miniText.deserialize(mainConfig.messages.alreadyInClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val newClan = clanManager.createNewClan(tag, user)
            clanManager.saveClan(newClan)

            val updatedUser = user.copy(clanTag = tag)
            userManager.saveUser(updatedUser)

            miniText.deserialize(mainConfig.messages.createdClan).component().let {
                player.sendMessage(it)
            }
        }
    }

    @Execute(name = "pvp")
    fun pvpCommand(
        @Context player: Player,
        @Arg("akcja") action: String?
    ) {
        scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val tag = user.clanTag
            if (tag == null) {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let { player.sendMessage(it) }
                return@launch
            }
            val clan = clanManager.getClan(tag) ?: run {
                miniText.deserialize(mainConfig.messages.clanNotFound).component().let { player.sendMessage(it) }
                return@launch
            }
            val role = clan.members[user.uuid]
            if (role == null || !hasRoleAtLeast(role, clan.pvpEditMinRole)) {
                miniText.deserialize(mainConfig.messages.pvpNoPermissionToggle).component()
                    .let { player.sendMessage(it) }
                return@launch
            }

            val newEnabled = when (action?.lowercase()) {
                null, "toggle" -> !clan.pvpEnabled
                "on", "wlacz" -> true
                "off", "wylacz" -> false
                else -> {
                    miniText.deserialize(mainConfig.messages.correctUsages + "\n/clan pvp [on|off|toggle]").component()
                        .let { player.sendMessage(it) }
                    return@launch
                }
            }

            clan.pvpEnabled = newEnabled
            clanManager.saveClan(clan)

            val msg = if (newEnabled) mainConfig.messages.pvpEnabledNow else mainConfig.messages.pvpDisabledNow
            miniText.deserialize(msg).component().let { player.sendMessage(it) }
        }
    }

    private fun hasRoleAtLeast(current: ClanRole, minimum: ClanRole): Boolean {
        fun weight(r: ClanRole) = when (r) {
            ClanRole.LEADER -> 3
            ClanRole.COLEADER -> 2
            ClanRole.MEMBER -> 1
        }
        return weight(current) >= weight(minimum)
    }

    @Execute(name = "zapros")
    fun inviteCommand(
        @Context player: Player,
        @Arg("gracz") targetPlayer: Player
    ) {
        scope.launch {
            val inviterUser = userManager.getUser(player.uniqueId)
            val targetUser = userManager.getUser(targetPlayer.uniqueId)

            val inviterClanTag = inviterUser.clanTag
            if (inviterClanTag == null) {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            if (targetUser.clanTag != null) {
                miniText.deserialize(mainConfig.messages.targetAlreadyInClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val inviterClan = clanManager.getClan(inviterClanTag) ?: run {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            if (inviterClan.members.size == inviterClan.maxSize) {
                miniText.deserialize(mainConfig.messages.maxSize).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val inviterRole = inviterClan.members[inviterUser.uuid]
            if (inviterRole == null || (inviterRole != ClanRole.LEADER && inviterRole != ClanRole.COLEADER)) {
                miniText.deserialize(mainConfig.messages.cantInvite).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clanManager.invitePlayer(inviterUser, targetUser, inviterClan)

            miniText.deserialize(
                mainConfig.messages.msgToInviter
                    .replace("[INVITED]", targetPlayer.name)
            ).component().let {
                player.sendMessage(it)
            }

            miniText.deserialize(
                mainConfig.messages.invitedToClan
                    .replace("[CLAN-TAG]", inviterClanTag)
                    .replace("[INVITER]", player.name)
            ).component().let {
                targetPlayer.sendMessage(it)
            }
        }
    }


    @Execute(name = "info")
    fun infoExecute(
        @Context player: Player,
        @Arg("tag") tag: String
    ) {
        this.scope.launch {
            val clan = clanManager.getClan(tag)
            if (clan == null) {
                miniText.deserialize(mainConfig.messages.clanNotFound).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val membersString = withContext(Dispatchers.IO) {
                clan.members.keys
                    .mapNotNull { uuid -> Bukkit.getOfflinePlayer(uuid).name }
                    .joinToString(mainConfig.clanSettings.separator)
            }

            val averagePoints = clanManager.averagePoints(clan, userManager)
            val coLeader =
                clan.members.entries.find { it.value == ClanRole.COLEADER }?.key?.let { Bukkit.getOfflinePlayer(it).name }
                    ?: "Brak"

            miniText.deserialize(
                mainConfig.messages.clanInfo
                    .replace("[TAG]", tag)
                    .replace("[MEMBERS]", membersString)
                    .replace("[POINTS]", averagePoints.toString())
                    .replace("[COLEADER]", coLeader)
                    .replace("[LEADER]", Bukkit.getOfflinePlayer(clan.ownerUuid).name!!)
            ).component().let {
                player.sendMessage(it)
            }
        }
    }

    @Execute(name = "dolacz")
    fun joinCommand(
        @Context player: Player
    ) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            if (user.clanTag != null) {
                miniText.deserialize(mainConfig.messages.alreadyInClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val success = clanManager.acceptInvite(user)
            if (success)
                miniText.deserialize(mainConfig.messages.joinedToClan).component().let {
                    player.sendMessage(it)
                }
            else
                miniText.deserialize(mainConfig.messages.cantJoin).component().let {
                    player.sendMessage(it)
                }
        }
    }

    @Execute(name = "opusc")
    fun leaveCommand(
        @Context player: Player
    ) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            if (user.clanTag == null) {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clanManager.getClan(user.clanTag!!).let {
                if (it!!.members[user.uuid] == ClanRole.LEADER) {
                    miniText.deserialize(mainConfig.messages.leaderCantLeave).component().let { message ->
                        player.sendMessage(message)
                    }
                    return@launch
                }

                it.members.remove(user.uuid)
                clanManager.saveClan(it)

                Bukkit.getPluginManager().callEvent(
                    UserQuitClanEvent(
                        user = user,
                        clan = it,
                        clanQuitCause = ClanQuitCause.LEAVE
                    )
                )
            }

            user.clanTag = null
            userManager.saveUser(user)

            miniText.deserialize(mainConfig.messages.leftClan).component().let {
                player.sendMessage(it)
            }
        }
    }

    @Execute(name = "usun")
    fun deleteCommand(@Context player: Player) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val clanTag = user.clanTag
            if (clanTag == null) {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clanManager.getClan(clanTag)?.let { clan ->
                clan.members[user.uuid]?.let { member ->
                    if (member != ClanRole.LEADER) {
                        miniText.deserialize(mainConfig.messages.notLeader).component().let {
                            player.sendMessage(it)
                        }
                        return@launch
                    }
                    if (clanManager.deleteRequest(user, clan)) {
//                        clan.members.forEach { member ->
//                            userManager.getUser(member.key).clanTag = null
//                        }
                        clanManager.deleteClan(clan)
                    } else
                        miniText.deserialize(mainConfig.messages.requestDelete).component().let {
                            player.sendMessage(it)
                        }
                }
            }
        }
    }

    @Execute(name = "zastepca")
    fun coleaderCommand(
        @Context player: Player,
        @Arg("gracz") target: Player
    ) {
        if(player == target) {
            this.miniText.deserialize(this.mainConfig.messages.selfPromotion).component().let {
                player.sendMessage(it)
            }
            return
        }

        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val targetUser = userManager.getUser(target.uniqueId)

            val clanTag = user.clanTag ?: run {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            if (targetUser.clanTag != clanTag) {
                miniText.deserialize(mainConfig.messages.notSameClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val clan = clanManager.getClan(clanTag) ?: run {
                miniText.deserialize(mainConfig.messages.clanNotFound).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clan.members[targetUser.uuid].let {
                if (it == ClanRole.COLEADER || it == ClanRole.LEADER) {
                    miniText.deserialize(mainConfig.messages.targetIsAlreadyColeader).component().let { message ->
                        player.sendMessage(message)
                    }
                    return@launch
                }
            }


            clan.members[user.uuid].let {
                if (it != ClanRole.LEADER) {
                    miniText.deserialize(mainConfig.messages.cantPromote).component().let { message ->
                        player.sendMessage(message)
                    }
                    return@launch
                }
            }


            clan.members.entries.find { it.value == ClanRole.COLEADER }.let {
                if (it != null) {
                    if (it.key == targetUser.uuid) {
                        miniText.deserialize(mainConfig.messages.targetIsAlreadyColeader).component().let {
                            player.sendMessage(it)
                        }
                        return@launch
                    }

                    clan.members[it.key] = ClanRole.MEMBER
                }
            }

            clan.members[targetUser.uuid] = ClanRole.COLEADER
            clanManager.saveClan(clan)

            miniText.deserialize(
                mainConfig.messages.promotedToColeaderSuccess
                    .replace("[PLAYER]", target.name)
            ).component().let {
                player.sendMessage(it)
            }

            miniText.deserialize(
                mainConfig.messages.youWerePromotedToColeader
                    .replace("[PLAYER]", target.name)
            ).component().let {
                target.sendMessage(it)
            }
        }
    }

    @Execute(name = "wyrzuc")
    fun kickCommand(
        @Context player: Player,
        @Arg("gracz") target: Player
    ) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val targetUser = userManager.getUser(target.uniqueId)

            if (user.clanTag == null) {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            if (user.clanTag != targetUser.clanTag) {
                miniText.deserialize(mainConfig.messages.notSameClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            if (player == target) {
                miniText.deserialize(mainConfig.messages.selfKick).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }


            clanManager.getClan(user.clanTag!!).let { clan ->
                val playerRole = clan!!.members[player.uniqueId]
                (playerRole == ClanRole.LEADER || playerRole == ClanRole.COLEADER).let {
                    if (!it) {
                        miniText.deserialize(mainConfig.messages.notEnoughPermission).component().let { message ->
                            player.sendMessage(message)
                        }
                        return@launch
                    }
                }

                if (clan.members[targetUser.uuid] == ClanRole.LEADER) {
                    miniText.deserialize(mainConfig.messages.cantKick).component().let {
                        player.sendMessage(it)
                    }
                    return@launch
                }

                clan.members.remove(target.uniqueId)
                Bukkit.getPluginManager().callEvent(
                    UserQuitClanEvent(
                        user = user,
                        clan = clan,
                        clanQuitCause = ClanQuitCause.KICK
                    )
                )
                clanManager.saveClan(clan)

                targetUser.clanTag = null
                userManager.saveUser(targetUser)
            }

            miniText.deserialize(
                mainConfig.messages.kickedSuccess
                    .replace("[PLAYER]", target.name)
            ).component().let {
                player.sendMessage(it)
            }

            miniText.deserialize(
                mainConfig.messages.youWerekicked
                    .replace("[TAG]", user.clanTag!!)
            ).component().let {
                target.sendMessage(it)
            }
        }
    }

    @Execute(name = "panel")
    fun executePanel(@Context player: Player) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val clanTag = user.clanTag ?: run {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let { message ->
                    player.sendMessage(message)
                }
                return@launch
            }

            val clan = clanManager.getClan(clanTag) ?: run {
                miniText.deserialize(mainConfig.messages.clanNotFound).component().let { message ->
                    player.sendMessage(message)
                }
                return@launch
            }

            ClanMenu(
                inventoryManager = inventoryManager,
                miniText = miniText,
                guiConfiguration = guiConfiguration,
                clanManager = clanManager,
                userManager = userManager,
                mainConfig = mainConfig,
                scope = scope
            ).open(player)
        }
    }
}
