package live.maquq.spigot.clans.commands

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.api.common.ClanRole
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.UserManager
import live.maquq.spigot.clans.manager.clan.ClanManager
import org.bukkit.entity.Player
import kotlin.random.Random

enum class ModifyType {
    ADD,
    REMOVE,
    SET
}

enum class RankType {
    POINTS,
    DEATHS,
    KILLS
}

@Command(name = "adminclan" , aliases = ["aclan"])
class AdminClanCommand(
    private val userManager: UserManager,
    private val clanManager: ClanManager,
    private val miniText: MiniText,
    private val mainConfig: PluginConfiguration,
    private val scope: CoroutineScope
) {

    @Execute(name = "user")
    fun userRanking(@Context player: Player,
                    @Arg("add/remove/set") modifyType: ModifyType,
                    @Arg("points/deaths/kills") rankType: RankType,
                    @Arg("gracz") target: Player,
                    @Arg("ilosc") amount: Int) {
        this.scope.launch {
            val user = userManager.getUser(target.uniqueId)

            when (rankType) {
                RankType.POINTS -> {
                    when (modifyType) {
                        ModifyType.ADD -> user.points += amount
                        ModifyType.REMOVE -> user.points = maxOf(0, user.points - amount)
                        ModifyType.SET -> user.points = maxOf(0, amount)
                    }
                }
                RankType.DEATHS -> {
                    when (modifyType) {
                        ModifyType.ADD -> user.deaths += amount
                        ModifyType.REMOVE -> user.deaths = maxOf(0, user.deaths - amount)
                        ModifyType.SET -> user.deaths = maxOf(0, amount)
                    }
                }
                RankType.KILLS -> {
                    when (modifyType) {
                        ModifyType.ADD -> user.kills += amount
                        ModifyType.REMOVE -> user.kills = maxOf(0, user.kills - amount)
                        ModifyType.SET -> user.kills = maxOf(0, amount)
                    }
                }
            }

            userManager.saveUser(user)
            miniText.deserialize(mainConfig.messages.adminChangeSuccess).component().let {
                player.sendMessage(it)
            }
        }
    }

    @Execute(name = "delete")
    fun deleteClan(@Context player: Player, @Arg("tag") tag: String) {
        this.scope.launch {
            val clan = clanManager.getClan(tag) ?: run {
                miniText.deserialize(mainConfig.messages.clanNotFound).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clanManager.deleteClan(clan)
            miniText.deserialize(mainConfig.messages.adminChangeSuccess).component().let {
                player.sendMessage(it)
            }
        }
    }

    @Execute(name = "setowner")
    fun setOwner(@Context player: Player, @Arg("tag") tag: String, @Arg("newOwner") newOwner: Player) {
        this.scope.launch {
            val clan = clanManager.getClan(tag) ?: run {
                miniText.deserialize(mainConfig.messages.clanNotFound).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            val targetUser = userManager.getUser(newOwner.uniqueId)

            if (targetUser.clanTag != null && targetUser.clanTag != tag) {
                miniText.deserialize(mainConfig.messages.targetAlreadyInClan).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            if (clan.ownerUuid == newOwner.uniqueId) {
                miniText.deserialize(mainConfig.messages.targetIsAlreadyColeader).component().let {
                    player.sendMessage(it)
                }
                return@launch
            }

            clan.members[clan.ownerUuid] = ClanRole.MEMBER

            clan.members[newOwner.uniqueId] = ClanRole.LEADER
            clan.ownerUuid = newOwner.uniqueId

            if (targetUser.clanTag != tag) {
                targetUser.clanTag = tag
                userManager.saveUser(targetUser)
            }

            clanManager.saveClan(clan)

            miniText.deserialize(mainConfig.messages.adminChangeSuccess).component().let {
                player.sendMessage(it)
            }
        }
    }

    @Execute(name = "randomizeStats")
    fun executeRandomize(@Context player: Player) {
        this.scope.launch {
            val user = userManager.getUser(player.uniqueId)
            user.kills = Random.nextInt(0, 1000)
            user.deaths = Random.nextInt(0, 1000)
            userManager.saveUser(user)

            miniText.deserialize(mainConfig.messages.adminChangeSuccess).component().let {
                player.sendMessage(it)
            }
        }
    }
}