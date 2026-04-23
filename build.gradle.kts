plugins {
    kotlin("jvm") version "2.3.21" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    kotlin("plugin.serialization") version "1.9.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")

    group = "live.maquq"
    version = "1.0"

}