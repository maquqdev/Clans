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
        version = "0.1-DEV"
        main = "live.maquq.spigot.clans.ClansPlugin"
        depend = listOf("Vault")
        authors = listOf("maquq")
    }

    server {
        version = "1.20.1"

//      port = 55555
        eula = true
        onlineMode = true

        debug = false
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    implementation(project(":storage"))
    implementation(project(":gui"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("dev.rollczi:litecommands-bukkit:3.10.4")
    implementation("com.github.Bruhdows:MiniText:v1.0.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
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

        archiveBaseName.set("Clans")
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}