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
import live.maquq.api.clan.ClanRole
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.clan.ClanManager
import live.maquq.spigot.clans.manager.UserManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@Command(name = "klan")
class ClanCommand(
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration,
    private val scope: CoroutineScope,
    private val clanManager: ClanManager,
    private val userManager: UserManager
) {

    @Execute(name = "stworz")
    fun execute(
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
            val coLeader = clan.members.entries.find { it.value == ClanRole.COLEADER }?.key?.let { Bukkit.getOfflinePlayer(it).name } ?: "Brak"

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
            if (success) {
                miniText.deserialize(mainConfig.messages.joinedToClan).component().let {
                    player.sendMessage(it)
                }
            } else {
                miniText.deserialize(mainConfig.messages.cantJoin).component().let {
                    player.sendMessage(it)
                }
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
                    if (clanManager.deleteRequest(user, clan))
                        clanManager.deleteClan(clan)
                    else {
                        miniText.deserialize(mainConfig.messages.requestDelete).component().let {
                            player.sendMessage(it)
                        }
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
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            val targetUser = userManager.getUser(target.uniqueId)

            val clanTag = user.clanTag
            if(clanTag == null) {
                miniText.deserialize(mainConfig.messages.notInAnyClan).component().let {
                    player.sendMessage(it)
                }
            }

            targetUser.clanTag ?: miniText.deserialize(mainConfig.messages.targetNotInAnyClan).component().let {
                player.sendMessage(it)
            }

            if (targetUser.clanTag != user.clanTag) {
                miniText.deserialize(mainConfig.messages.notSameClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clanManager.getClan(clanTag!!).let { it ->
                it!!.members[user.uuid]?.let { member ->
                    if(member != ClanRole.LEADER) {
                        miniText.deserialize(mainConfig.messages.notLeader).component().let { message ->
                            player.sendMessage(message)
                        }
                        return@launch
                    }

                    it.members[targetUser.uuid] = ClanRole.LEADER
                    println("zmieniono lidera")
                    clanManager.saveClan(it)
                }
            }
        }
    }

    @Execute(name = "debug")
    fun debug(@Context player: Player) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)

            println("informacje o ${player.name}!!!")
            if (user.clanTag != null)
                println("ma klan ${user.clanTag}")
            else
                println("nie ma klanu")
            println(user.kills)
            println(user.deaths)
        }
    }
}
