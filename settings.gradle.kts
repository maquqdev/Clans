plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi")
        maven("https://repo.panda-lang.org/releases")
        maven("https://jitpack.io")
    }
}


rootProject.name = "Clans-parent"

include(":api", ":gui", ":storage", ":spigot")
