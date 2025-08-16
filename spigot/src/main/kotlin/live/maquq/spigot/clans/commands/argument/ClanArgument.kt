package live.maquq.spigot.clans.commands.argument

import com.bruhdows.minitext.MiniText
import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.shared.FailedReason
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import live.maquq.api.clan.Clan
import live.maquq.spigot.clans.configuration.impl.PluginConfiguration
import live.maquq.spigot.clans.manager.ClanManager
import org.bukkit.command.CommandSender

class ClanArgument(
    private val clanManager: ClanManager,
    private val scope: CoroutineScope,
    private val mainConfig: PluginConfiguration,
    private val miniText: MiniText
) : ArgumentResolver<CommandSender, Clan>() {

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<Clan>,
        argument: String
    ): ParseResult<Clan> {
        return runBlocking {
            val clan = clanManager.getClan(argument)
            if (clan == null) {
                val translated = miniText.deserialize(mainConfig.messages.clanNotFound).component()
                invocation.sender().sendMessage(translated)
                ParseResult.failure("Clan not found")
            } else
                ParseResult.success(clan)
        }
    }

    override fun suggest(
        invocation: Invocation<CommandSender>?,
        argument: Argument<Clan>?,
        context: SuggestionContext?
    ): SuggestionResult {
        return SuggestionResult.of(this.clanManager.getAllClans().map { it.tag })
    }
}