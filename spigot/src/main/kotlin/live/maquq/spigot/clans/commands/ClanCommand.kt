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
import live.maquq.spigot.clans.manager.ClanManager
import live.maquq.spigot.clans.manager.UserManager
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@Command(name = "clan")
class ClanCommand(
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration,
    private val scope: CoroutineScope,

    private val clanManager: ClanManager,
    private val userManager: UserManager
) {

    @Execute(name = "create")
    fun execute(
        @Context player: Player,
        @Arg("tag") tag: String
    ) {
        scope.launch {
            if (clanManager.getClan(tag) != null) {
                val translated = miniText.deserialize(mainConfig.messages.clanAlreadyExists).component()
                player.sendMessage(translated)
                return@launch
            }

            val user = userManager.getUser(player.uniqueId)

            if (user.clanTag != null) {
                val translated = miniText.deserialize(mainConfig.messages.alreadyInClan).component()
                player.sendMessage(translated)
                return@launch
            }

            val newClan = clanManager.createNewClan(tag, user)
            clanManager.saveClan(newClan)

            val updatedUser = user.copy(clanTag = tag)
            userManager.saveUser(updatedUser)

            val translated = miniText.deserialize(mainConfig.messages.createdClan).component()
            player.sendMessage(translated)
        }
    }

    @Execute(name = "debug")
    fun debug(@Context player: Player) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)

            println("informacje o ${player.name}!!!")
            if(user.clanTag != null)
                println("ma klan ${user.clanTag}")
            else
                println("nie ma klanu")
            println(user.kills)
            println(user.deaths)
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
                val translated = miniText.deserialize(mainConfig.messages.notInAnyClan).component()
                player.sendMessage(translated)
                return@launch
            }

            if (targetUser.clanTag != null) {
                val translated = miniText.deserialize(mainConfig.messages.targetAlreadyInClan).component()
                player.sendMessage(translated)
                return@launch
            }

            val inviterClan = clanManager.getClan(inviterClanTag) ?: run {
                val translated = miniText.deserialize(mainConfig.messages.notInAnyClan).component()
                player.sendMessage(translated)
                return@launch
            }

            if(inviterClan.members.size )

            val inviterRole = inviterClan.members[inviterUser.uuid]
            if (inviterRole == null || (inviterRole != ClanRole.LEADER && inviterRole != ClanRole.VLEADER)) {
                val translated = miniText.deserialize(mainConfig.messages.cantInvite)
                return@launch
            }

            clanManager.invitePlayer(inviterUser, targetUser, inviterClan)

            val translated = miniText.deserialize(mainConfig.messages.msgToInviter).component()
            player.sendMessage(translated)

            val translatedToTarget = miniText.deserialize(mainConfig.messages.invitedToClan
                .replace("[CLAN-TAG]", inviterClanTag)
                .replace("[INVITER]", player.name)
            ).component()
            targetPlayer.sendMessage(translatedToTarget)
        }
    }

    @Execute(name = "info")
    fun infoExecute(
        @Context player: Player,
        @Arg("tag") tag: String
    ) {
        this.scope.launch {
            val clan = clanManager.getClan(tag)
            if(clan == null) {
                val translated = miniText.deserialize(mainConfig.messages.clanNotFound).component()
                player.sendMessage(translated)
                return@launch
            }

            val membersString = withContext(Dispatchers.IO) {
                clan.members.keys
                    .mapNotNull { uuid -> Bukkit.getOfflinePlayer(uuid).name }
                    .joinToString(mainConfig.clanSettings.separator)
            }

            val averagePoints = clanManager.averagePoints(clan, userManager)

            val translated = miniText.deserialize(
                mainConfig.messages.clanInfo
                    .replace("[TAG]", tag)
                    .replace("[MEMBERS]", membersString)
                    .replace("[POINTS]", averagePoints.toString())
            ).component()
            player.sendMessage(translated)
        }
    }


    @Execute(name = "join")
    fun joinCommand(
        @Context player: Player,
        @Arg tag: String
    ) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId) ?: return@launch

            if (user.clanTag != null) {
                player.sendMessage(miniText.deserialize("[red]Jesteś już w klanie!").component())
                return@launch
            }

            val success = clanManager.acceptInvite(user)
            if (success) {
                player.sendMessage(miniText.deserialize("[green]Pomyślnie dołączono do klanu!").component())
            } else {
                player.sendMessage(miniText.deserialize("[red]Nie masz żadnych oczekujących zaproszeń lub zaproszenie wygasło.").component())
            }
        }
    }
}