import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("ru.endlesscode.bukkitgradle") version "1.0.0"
    kotlin("plugin.serialization")
}

group = "live.maquq"
version = "1.0"
description = "klany cos tam trybia ale nw"

bukkit {
    apiVersion = "1.19.4"

    plugin {
        name = "Clans"
        version = "1.0"
        main = "live.maquq.spigot.clans.ClansPlugin"
        depend = listOf("PlaceholderAPI")
        authors = listOf("maquq")
    }

    server {
        version = "1.20.1"

        eula = true
        onlineMode = true

        debug = false
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.7")

    implementation(project(":storage"))
    implementation(project(":gui"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("dev.rollczi:litecommands-bukkit:3.10.4")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("org.yaml:snakeyaml:2.0")
}

tasks {
    named<ShadowJar>("shadowJar") {
        relocate("kotlin", "live.maquq.libs.kotlin")
        relocate("org.jetbrains", "live.maquq.libs.jetbrains")
        relocate("org.intellij", "live.maquq.libs.intellij")
        relocate("kotlinx.coroutines", "live.maquq.libs.coroutines")
        relocate("_COROUTINE", "live.maquq.libs.coroutines")
        relocate("dev.rollczi.litecommands", "live.maquq.libs.litecommands")
        relocate("com.bruhdows.minitext", "live.maquq.libs.minitext")
        relocate("net.kyori.examination", "live.maquq.libs.examination")
        relocate("com.zaxxer.hikari", "live.maquq.libs.hikaricp")
        relocate("org.mongodb", "live.maquq.libs.mongodb")
        relocate("org.bson", "live.maquq.libs.bson")
        relocate("com.mysql", "live.maquq.libs.mysql")
        relocate("com.google.protobuf", "live.maquq.libs.protobuf")
        relocate("org.json.simple", "live.maquq.libs.json")
        relocate("org.slf4j", "live.maquq.libs.slf4j")
        relocate("org.yaml.snakeyaml", "live.maquq.libs.snakeyaml")
        relocate("org.yaml.snakeyaml", "live.maquq.libs.snakeyaml")

        archiveBaseName.set("Clans")
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}

