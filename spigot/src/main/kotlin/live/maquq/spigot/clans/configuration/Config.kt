package live.maquq.spigot.clans.configuration

import com.google.gson.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import live.maquq.spigot.clans.BukkitLogger
import live.maquq.spigot.clans.configuration.adapter.ItemStackAdapter
import live.maquq.spigot.clans.configuration.adapter.OptionalAdapter
import org.bukkit.inventory.ItemStack
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Modifier

class Config<T : ConfigTemplate>(
    private val configClass: Class<T>,
    private val file: File,
    private val logger: BukkitLogger,
    private val format: ConfigFormat = ConfigFormat.AUTO
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeHierarchyAdapter(ItemStack::class.java, ItemStackAdapter())
        .registerTypeAdapterFactory(OptionalAdapter.FACTORY)
        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    private val yaml: Yaml = DumperOptions().let { opts ->
        opts.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        opts.isPrettyFlow = true
        opts.indent = 2
        Yaml(opts)
    }

    lateinit var get: T
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

    private lateinit var effectiveFile: File
    private lateinit var effectiveFormat: ConfigFormat

    init {
        resolveEffectiveTarget()
        loadFromFile()
    }

    private fun resolveEffectiveTarget() {
        val parent = file.parentFile
        if (!parent.exists()) parent.mkdirs()

        val ymlFile = if (file.name.endsWith(".yml", ignoreCase = true)) file else File(parent, changeExtension(file.name, ".yml"))
        val jsonFile = if (file.name.endsWith(".json", ignoreCase = true)) file else File(parent, changeExtension(file.name, ".json"))

        when (format) {
            ConfigFormat.YAML -> {
                effectiveFormat = ConfigFormat.YAML
                effectiveFile = ymlFile
            }
            ConfigFormat.JSON -> {
                effectiveFormat = ConfigFormat.JSON
                effectiveFile = jsonFile
            }
            ConfigFormat.AUTO -> {
                when {
                    ymlFile.exists() -> { effectiveFormat = ConfigFormat.YAML; effectiveFile = ymlFile }
                    jsonFile.exists() -> { effectiveFormat = ConfigFormat.JSON; effectiveFile = jsonFile }
                    else -> { effectiveFormat = ConfigFormat.YAML; effectiveFile = ymlFile }
                }
            }
        }
        logger.debug("Config effective format=${effectiveFormat} file='${effectiveFile.name}' (requested='${file.name}')")
    }

    private fun loadFromFile() {
        if (!effectiveFile.parentFile.exists())
            effectiveFile.parentFile.mkdirs()

        val hasComments = hasCommentAnnotations()

        when (effectiveFormat) {
            ConfigFormat.YAML -> {
                val legacyJson = if (!effectiveFile.exists()) File(effectiveFile.parentFile, changeExtension(effectiveFile.name, ".json")) else null
                if (effectiveFile.exists()) {
                    runCatching {
                        FileReader(effectiveFile).use { reader ->
                            val loaded: Any? = yaml.load(reader)
                            val jsonTree = plainToJson(loaded)
                            this.get = gson.fromJson(jsonTree, configClass)
                        }
                        logger.debug("YAML '${effectiveFile.name}' has been loaded.")
                    }.onFailure {
                        logger.error("Cannot load YAML '${effectiveFile.name}'! Creating default.", it)
                        createDefault()
                    }
                } else if (legacyJson != null && legacyJson.exists()) {
                    logger.debug("Migrating legacy '${legacyJson.name}' -> '${effectiveFile.name}'.")
                    runCatching {
                        FileReader(legacyJson).use { reader ->
                            this.get = gson.fromJson(reader, configClass)
                        }
                        saveToFile()
                    }.onFailure {
                        logger.error("Cannot migrate from legacy '${legacyJson.name}'! Creating default.", it)
                        createDefault()
                    }
                } else {
                    logger.debug("Creating new YAML '${effectiveFile.name}'.")
                    createDefault()
                }
            }
            ConfigFormat.JSON -> {
                if (effectiveFile.exists()) {
                    if (hasComments) {
                        logger.warn("Config model declares @Comment annotations but format is JSON. Skipping load of '${effectiveFile.name}' and using defaults. Switch to YAML to enable comments.")
                        createDefault(false)
                    } else {
                        runCatching {
                            FileReader(effectiveFile).use { reader ->
                                this.get = gson.fromJson(reader, configClass)
                            }
                            logger.debug("JSON '${effectiveFile.name}' has been loaded.")
                        }.onFailure {
                            logger.error("Cannot load JSON '${effectiveFile.name}'! Creating default.", it)
                            createDefault()
                        }
                    }
                } else {
                    if (hasComments) {
                        logger.warn("Config model declares @Comment annotations but format is JSON. Creating defaults in memory only; switch to YAML to persist with comments.")
                        createDefault(save = false)
                    } else {
                        logger.debug("Creating new JSON '${effectiveFile.name}'.")
                        createDefault(save = true)
                    }
                }
            }
            ConfigFormat.AUTO -> {
                effectiveFormat = ConfigFormat.YAML
                loadFromFile()
            }
        }
    }

    private fun createDefault(save: Boolean = true) {
        runCatching {
            this.get = configClass.getConstructor().newInstance()
            if (save) saveToFile()
        }.onFailure {
            logger.error("Cannot create a new instance to file '${effectiveFile.name}'!", it)
        }
    }

    fun save() = scope.launch {
        saveToFile()
    }

    fun reload() {
        logger.debug("Reloading '${file.name}'...")
        resolveEffectiveTarget()
        loadFromFile()
    }

    private fun saveToFile() {
        runCatching {
            when (effectiveFormat) {
                ConfigFormat.YAML -> {
                    val tree: JsonElement = gson.toJsonTree(this.get)
                    val plain: Any? = jsonToPlain(tree)
                    var yamlText = yaml.dump(plain)
                    val comments = collectYamlComments()
                    if (comments.isNotEmpty()) {
                        yamlText = injectYamlComments(yamlText, comments)
                    }
                    FileWriter(effectiveFile).use { writer ->
                        writer.write(yamlText)
                    }
                }
                ConfigFormat.JSON -> {
                    FileWriter(effectiveFile).use { writer ->
                        gson.toJson(this.get, writer)
                    }
                }
                ConfigFormat.AUTO -> {
                    // Should not happen; resolveEffectiveTarget sets concrete format
                    FileWriter(effectiveFile).use { writer ->
                        val tree: JsonElement = gson.toJsonTree(this.get)
                        val plain: Any? = jsonToPlain(tree)
                        writer.write(yaml.dump(plain))
                    }
                }
            }
            logger.debug("'${effectiveFile.name}' has been saved.")
        }.onFailure {
            logger.error("An error occurred while saving '${effectiveFile.name}'!", it)
        }
    }

    fun shutdown() {
        scope.cancel()
    }

    private fun changeExtension(name: String, newExt: String): String {
        val idx = name.lastIndexOf('.')
        return if (idx != -1) name.take(idx) + newExt else name + newExt
    }

    private fun jsonToPlain(element: JsonElement): Any? = when {
        element.isJsonNull -> null
        element.isJsonPrimitive -> {
            val prim = element.asJsonPrimitive
            when {
                prim.isBoolean -> prim.asBoolean
                prim.isNumber -> prim.asNumber
                else -> prim.asString
            }
        }
        element.isJsonArray -> {
            val list = mutableListOf<Any?>()
            element.asJsonArray.forEach { list.add(jsonToPlain(it)) }
            list
        }
        element.isJsonObject -> {
            val map = linkedMapOf<String, Any?>()
            for ((k, v) in element.asJsonObject.entrySet()) {
                map[k] = jsonToPlain(v)
            }
            map
        }
        else -> null
    }

    private fun plainToJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull.INSTANCE
        is JsonElement -> value
        is Map<*, *> -> JsonObject().apply {
            value.forEach { (k, v) ->
                if (k != null) add(k.toString(), plainToJson(v))
            }
        }
        is Iterable<*> -> JsonArray().apply {
            value.forEach { add(plainToJson(it)) }
        }
        is Array<*> -> JsonArray().apply {
            value.forEach { add(plainToJson(it)) }
        }
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Char -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        else -> gson.toJsonTree(value)
    }

    // --- YAML comments support (top-level keys) ---
    private fun hasCommentAnnotations(): Boolean {
        return try {
            hasCommentAnnotationsRecursive(configClass, HashSet())
        } catch (_: Throwable) {
            false
        }
    }

    private fun hasCommentAnnotationsRecursive(type: Class<*>, visited: MutableSet<Class<*>>): Boolean {
        if (!visited.add(type)) return false
        for (f in type.declaredFields) {
            if (f.getAnnotation(Comment::class.java) != null) return true
            val ft = f.type
            if (ConfigTemplate::class.java.isAssignableFrom(ft)) {
                if (hasCommentAnnotationsRecursive(ft, visited)) return true
            }
        }
        return false
    }

    private fun collectYamlComments(): Map<String, String> {
        val map = linkedMapOf<String, String>()
        try {
            for (field in configClass.declaredFields) {
                val ann = field.getAnnotation(Comment::class.java) ?: continue
                map[field.name] = ann.value
            }
        } catch (_: Throwable) { /* ignore */ }
        return map
    }

    private fun injectYamlComments(yamlText: String, comments: Map<String, String>): String {
        if (comments.isEmpty()) return yamlText
        val lines = yamlText.lines().toMutableList()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            // Match top-level keys: no leading spaces and contains ':'
            if (!line.startsWith(" ") && line.contains(":")) {
                val key = line.substringBefore(":").trim()
                val comment = comments[key]
                if (comment != null) {
                    val commentLine = "# " + comment.replace("\n", "\n# ")
                    val needsInsert = when {
                        i == 0 -> true
                        else -> !lines[i - 1].trimStart().startsWith("# ") || lines[i - 1].trimStart() != commentLine
                    }
                    if (needsInsert) {
                        lines.add(i, commentLine)
                        i += 1 // Skip over inserted comment
                    }
                }
            }
            i += 1
        }
        return lines.joinToString("\n")
    }
}