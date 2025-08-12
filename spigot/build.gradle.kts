import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("ru.endlesscode.bukkitgradle") version "1.0.0"
}

group = "live.maquq"
version = "1.0"
description = "klany cos tam trybia ale nw"

bukkit {
    apiVersion = "1.19.4"

    plugin {
        name = "Clans"
        main = "live.maquq.spigot.clans.ClansPlugin"
        authors = listOf("maquq")
    }

    server {
        version = "1.20.1"

        eula = true

        onlineMode = true

        debug = true
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    implementation(project(":storage"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.uchuhimo:konf:1.1.2")
    implementation("com.uchuhimo:konf-yaml:1.1.2")

    tasks {
        named<ShadowJar>("shadowJar") {
            relocate("kotlinx.coroutines", "live.maquq.libs.coroutines")
            relocate("_COROUTINE", "live.maquq.libs.coroutines")
            relocate("com.google.gson", "live.maquq.libs.gson")
            relocate("com.zaxxer.hikari", "live.maquq.libs.hikaricp")
            relocate("org.mongodb", "live.maquq.libs.mongodb")
            relocate("org.bson", "live.maquq.libs.bson")
            relocate("com.mysql", "live.maquq.libs.mysql")

            archiveBaseName.set("Clans")
            archiveClassifier.set("")
        }

        build {
            dependsOn(shadowJar)
        }
    }
}