package live.maquq.spigot.clans.configuration.adapter

import com.google.gson.*
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type

class ItemStackAdapter : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    private val miniMessage = MiniMessage.miniMessage()

    override fun serialize(item: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("material", item.type.name)
        obj.addProperty("amount", item.amount)

        if (item.hasItemMeta()) {
            val meta = item.itemMeta!!

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

            if (meta.hasEnchants()) {
                val enchantsObj = JsonObject()
                meta.enchants.forEach { (enchant, level) ->
                    enchantsObj.addProperty(enchant.key.toString(), level)
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
        val amount = obj.get("amount")?.asInt ?: 1

        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item

        obj.get("name")?.asString?.let { meta.displayName(miniMessage.deserialize(it)) }

        obj.get("lore")?.asJsonArray?.map { element ->
            miniMessage.deserialize(element.asString)
        }?.let { meta.lore(it) }

        obj.get("enchants")?.asJsonObject?.entrySet()?.forEach { entry ->
            val enchant = when {
                entry.key.contains(":") -> {
                    val parts = entry.key.split(":", limit = 2)
                    Enchantment.getByKey(NamespacedKey(parts[0], parts[1]))
                }
                else -> {
                    Enchantment.getByKey(NamespacedKey.minecraft(entry.key.lowercase()))
                        ?: Enchantment.getByName(entry.key.uppercase())
                }
            }

            enchant?.let {
                val level = entry.value.asInt
                meta.addEnchant(it, level, true)
            }
        }

        item.itemMeta = meta
        return item
    }
}