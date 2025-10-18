package live.maquq.spigot.clans.configuration.adapter

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType
import java.util.Optional

class OptionalAdapter<E : Any>(private val adapter: TypeAdapter<E>) : TypeAdapter<Optional<E>>() {

    companion object {
        @JvmField
        val FACTORY: TypeAdapterFactory = object : TypeAdapterFactory {
            override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                if (type.rawType != Optional::class.java) {
                    return null
                }

                val parameterizedType = type.type as ParameterizedType
                val actualType = parameterizedType.actualTypeArguments[0]
                val adapter = gson.getAdapter(TypeToken.get(actualType))

                @Suppress("UNCHECKED_CAST")
                return OptionalAdapter(adapter) as TypeAdapter<T>
            }
        }
    }

    override fun write(writer: JsonWriter, value: Optional<E>?) {
        if (value == null || !value.isPresent) {
            writer.nullValue()
        } else {
            adapter.write(writer, value.get())
        }
    }

    override fun read(reader: JsonReader): Optional<E> {
        return Optional.ofNullable(adapter.read(reader))
    }
}