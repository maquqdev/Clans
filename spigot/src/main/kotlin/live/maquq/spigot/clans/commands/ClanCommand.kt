package live.maquq.spigot.clans.commands

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.ClanManager
import live.maquq.spigot.clans.manager.UserManager
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
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
        @Arg tag: String
    ) {
        this.scope.launch {
            if (clanManager.getClan(tag) != null) {
                val translated = miniText.deserialize(mainConfig.messages.clanAlreadyExists).component()
                player.sendMessage(translated)
                return@launch
            }

            val user = userManager.getUser(player.uniqueId)

            if (user.clanTag != null) {
                val message = miniText.deserialize(mainConfig.messages.alreadyInClan).component()
                player.sendMessage(message)
                return@launch
            }

            val newClan = clanManager.createNewClan(tag, user)
            clanManager.saveClan(newClan)

            val updatedUser = user.copy(clanTag = tag)
            updatedUser.init { newClanTag ->
                if (newClanTag == null) null else clanManager.getClan(newClanTag)
            }

            userManager.saveUser(updatedUser)

            val translated = miniText.deserialize(mainConfig.messages.createdClan).component()
            player.sendMessage(translated)
        }
    }

    @Execute(name = "zapros")
    fun inviteCommand(
        @Context player: Player,
        @Arg targetPlayer: Player
    ) {
        this.scope.launch {
            val inviter = userManager.getUser(player.uniqueId) ?: return@launch
            val targetUser = userManager.getUser(targetPlayer.uniqueId) ?: return@launch

            val clan = inviter.clan
            if (clan == null) {
                val translated = miniText.deserialize(mainConfig.messages.notInAnyClan).component()
                player.sendMessage(translated)
                return@launch
            }

            if (targetUser.clanTag != null) {
                val translated = miniText.deserialize(mainConfig.messages.targetAlreadyInClan).component()
                player.sendMessage(translated)
                return@launch
            }

            clanManager.invitePlayer(inviter, targetUser, clan)

            val translatedMsgToInviter = miniText.deserialize(mainConfig.messages.msgToInviter).component()
            player.sendMessage(translatedMsgToInviter)

            val translatedMsgToTarget = miniText.deserialize(
                mainConfig.messages.invitedToClan
                    .replace("[CLAN-TAG]", clan.tag)
                    .replace("[INVITER]", player.name)
            ).component()
            targetPlayer.sendMessage(translatedMsgToTarget)
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
                player.sendMessage(miniText.deserialize("<red>Jesteś już w klanie!").component())
                return@launch
            }

            val success = clanManager.acceptInvite(user)
            if (success) {
                player.sendMessage(miniText.deserialize("<green>Pomyślnie dołączono do klanu!").component())
            } else {
                player.sendMessage(miniText.deserialize("<red>Nie masz żadnych oczekujących zaproszeń lub zaproszenie wygasło.").component())
            }
        }
    }
}