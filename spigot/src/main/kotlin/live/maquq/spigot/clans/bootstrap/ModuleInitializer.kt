package live.maquq.spigot.clans.bootstrap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.maquq.spigot.clans.BukkitLogger

class ModuleInitializer(
    private val modules: List<PluginModule>,
    private val logger: BukkitLogger
) {
    private val enabledStack: MutableList<PluginModule> = mutableListOf()

    suspend fun enableAll(ctx: PluginContext) {
        for (module in modules) {
            val start = System.currentTimeMillis()
            runCatching {
                withContext(Dispatchers.Default) { module.enable(ctx) }
            }.onSuccess {
                enabledStack += module
                logger.debug("Enabled module '${module.name}' in ${System.currentTimeMillis() - start}ms")
            }.onFailure { ex ->
                logger.error("Failed to enable module '${module.name}', rolling back...", ex)
                runCatching { disableAll(ctx) }
                throw ex
            }
        }
    }

    suspend fun disableAll(ctx: PluginContext) {
        for (module in enabledStack.asReversed()) {
            runCatching {
                module.disable(ctx)
            }.onFailure { ex ->
                logger.error("Error while disabling module '${module.name}'", ex)
            }
        }
        enabledStack.clear()
    }
}