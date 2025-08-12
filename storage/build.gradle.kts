
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":api"))

    implementation(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    implementation("org.mongodb:mongodb-driver-sync:4.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    runtimeOnly("com.mysql:mysql-connector-j:8.0.33")
}