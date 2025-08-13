package live.maquq.spigot.clans.commands.argument

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import live.maquq.api.clan.Clan
import live.maquq.spigot.clans.manager.ClanManager
import org.bukkit.command.CommandSender

class ClanArgument(
    private val clanManager: ClanManager,
    private val scope: CoroutineScope
) : ArgumentResolver<CommandSender, Clan>() {

    override fun parse(
        invocation: Invocation<CommandSender>?,
        context: Argument<Clan>?,
        argument: String?
    ): ParseResult<Clan> {
//        this.scope.launch {
//            val clan = clanManager.getClan(argument!!)
//            if (clan != null)
//                return@launch ParseResult.success(clan)
//
//            return@launch ParseResult.failure("Clan not found")
//        }
//        return ParseResult.failure("Clan not found")
        return ParseResult.failure("keine worken")
    }

    override fun suggest(
        invocation: Invocation<CommandSender>?,
        argument: Argument<Clan>?,
        context: SuggestionContext?
    ): SuggestionResult {
        return SuggestionResult.empty()
    }
}