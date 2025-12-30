
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":api"))

    implementation(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    implementation("org.mongodb:mongodb-driver-sync:5.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jooq:jooq:3.19.1")
    implementation("org.jooq:jooq-kotlin:3.19.1")

    implementation("com.mysql:mysql-connector-j:8.4.0")

    implementation("com.zaxxer:HikariCP:7.0.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

