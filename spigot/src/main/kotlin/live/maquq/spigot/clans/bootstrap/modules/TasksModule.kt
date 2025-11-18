package live.maquq.spigot.clans.bootstrap.modules

import org.bukkit.Bukkit
import live.maquq.spigot.clans.bootstrap.PluginContext
import live.maquq.spigot.clans.bootstrap.PluginModule
import live.maquq.spigot.clans.task.DataSaveTask

class TasksModule : PluginModule {
    override val name: String = "Tasks"

    override suspend fun enable(ctx: PluginContext) {
        val taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
            ctx.plugin,
            DataSaveTask(
                ctx.scope,
                ctx.userManager,
                ctx.clanManager,
                ctx.logger
            ),
            20 * 60 * 15L,
            20 * 60 * 15L //15min
        ).taskId
        ctx.scheduledTaskIds += taskId
    }

    override suspend fun disable(ctx: PluginContext) {
        ctx.scheduledTaskIds.forEach {
            Bukkit.getScheduler().cancelTask(it)
        }
        ctx.scheduledTaskIds.clear()
    }
}