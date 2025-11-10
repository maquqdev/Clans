package live.maquq.spigot.clans.bootstrap

interface PluginModule {
    val name: String
    suspend fun enable(ctx: PluginContext)
    suspend fun disable(ctx: PluginContext)
}