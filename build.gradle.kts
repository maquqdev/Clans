plugins {
    kotlin("jvm") version "1.8.21" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")

    group = "live.maquq"
    version = "1.0"

}