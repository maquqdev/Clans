package live.maquq.spigot.clans.configuration.adapter

import com.google.gson.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.lang.reflect.Type

class ComponentAdapter : JsonSerializer<Component>, JsonDeserializer<Component> {

    private val miniMessage = MiniMessage.miniMessage()

    override fun serialize(src: Component, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val serialized = miniMessage.serialize(src)
        return JsonPrimitive(serialized)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Component {
        return miniMessage.deserialize(json.asString)
    }
}