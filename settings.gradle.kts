rootProject.name = "RemoteDexter-v2"
include("core", "cli")

// Centralized plugin version pins per Master Constitution
pluginManagement {
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    }
}
