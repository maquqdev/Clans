package live.maquq.spigot.clans.configuration.adapter

import com.google.gson.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type

class ItemStackAdapter : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    // Instancja MiniMessage jest teraz jedynym źródłem prawdy o (de)serializacji komponentów.
    private val miniMessage = MiniMessage.miniMessage()

    override fun serialize(item: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("material", item.type.name)

        if (item.hasItemMeta()) {
            val meta = item.itemMeta!!

            // --- JAWNA KONTROLA SERIALIZACJI ---
            if (meta.hasDisplayName()) {
                obj.addProperty("name", miniMessage.serialize(meta.displayName()!!))
            }
            if (meta.hasLore()) {
                val loreArray = JsonArray()
                meta.lore()?.forEach { component ->
                    loreArray.add(miniMessage.serialize(component))
                }
                obj.add("lore", loreArray)
            }
            // --- KONIEC ZMIANY ---

            if (meta.hasEnchants()) {
                val enchantsObj = JsonObject()
                meta.enchants.forEach { (enchant, level) ->
                    enchantsObj.addProperty(enchant.key.key, level)
                }
                obj.add("enchants", enchantsObj)
            }
        }
        return obj
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        if (!json.isJsonObject) throw JsonParseException("ItemStack must be a JSON object")
        val obj = json.asJsonObject

        val materialName = obj.get("material")?.asString ?: throw JsonParseException("ItemStack requires a 'material' field")
        val material = Material.matchMaterial(materialName) ?: Material.STONE
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        // --- JAWNA KONTROLA DESERIALIZACJI ---
        obj.get("name")?.asString?.let { meta.displayName(miniMessage.deserialize(it)) }

        obj.get("lore")?.asJsonArray?.map { element ->
            // Upewniamy się, że każdy element w liście jest traktowany jako string
            miniMessage.deserialize(element.asString)
        }?.let { meta.lore(it) }
        // --- KONIEC ZMIANY ---

        obj.get("enchants")?.asJsonObject?.entrySet()?.forEach { entry ->
            Enchantment.getByName(entry.key.uppercase())?.let { enchant ->
                val level = entry.value.asInt
                meta.addEnchant(enchant, level, true)
            }
        }

        item.itemMeta = meta
        return item
    }
}