plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    // Main entry is MainKt
    mainClass.set("com.remotedexter.cli.MainKt")
}
