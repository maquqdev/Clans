package live.maquq.api

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class LazyReference<out T : Any>(
    private val loader: suspend () -> T?
) {
    private val value: T? by lazy {
        runBlocking { loader() }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return value
    }
}