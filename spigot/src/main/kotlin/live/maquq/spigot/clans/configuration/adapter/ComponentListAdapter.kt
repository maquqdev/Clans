package live.maquq.spigot.clans.configuration.adapter

import com.google.gson.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.lang.reflect.Type

class ComponentListAdapter : JsonSerializer<List<Component>>, JsonDeserializer<List<Component>> {

    private val miniMessage = MiniMessage.miniMessage()

    override fun serialize(src: List<Component>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonArray = JsonArray()
        src.forEach { component ->
            jsonArray.add(miniMessage.serialize(component))
        }
        return jsonArray
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<Component> {
        val components = mutableListOf<Component>()
        json.asJsonArray.forEach { element ->
            components.add(miniMessage.deserialize(element.asString))
        }
        return components
    }
}