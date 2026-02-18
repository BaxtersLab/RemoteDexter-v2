rootProject.name = "RemoteDexter-v2"
include("core", "cli")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Centralized plugin version pins per Master Constitution
pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
