package live.maquq.spigot.clans.configuration

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.adapter.ComponentAdapter
import live.maquq.spigot.clans.configuration.adapter.ComponentListAdapter
import live.maquq.spigot.clans.configuration.adapter.ItemStackAdapter
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class Config<T : ConfigTemplate>(
    private val configClass: Class<T>,
    private val file: File,
    private val logger: BukkitLogger
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ItemStack::class.java, ItemStackAdapter())
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    lateinit var get: T
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

    init {
        loadFromFile()
    }

    private fun loadFromFile() {
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        if (file.exists()) {
            runCatching {
                FileReader(file).use { reader ->
                    this.get = gson.fromJson(reader, configClass)
                }
                logger.debug("File '${file.name}' has been loaded.")
            }.onFailure {
                logger.error("Cannot load '${file.name}'! - Creating a new, sorry!", it)
                createDefault()
            }
        } else {
            logger.debug("Creating a new file '${file.name}'.")
            createDefault()
        }
    }

    private fun createDefault() {
        runCatching {
            this.get = configClass.getConstructor().newInstance()
            saveToFile()
        }.onFailure {
            logger.error("Cannot create a new instance to file '${file.name}'!", it)
        }
    }

    fun save() = scope.launch {
        saveToFile()
    }

    fun reload() {
        logger.debug("Reloading '${file.name}'...")
        loadFromFile()
    }

    private fun saveToFile() {
        runCatching {
            FileWriter(file).use { writer ->
                gson.toJson(this.get, writer)
            }
            logger.debug("'${file.name}' has been loaded.")
        }.onFailure {
            logger.error("An error occurred while saving '${file.name}'!", it)
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}