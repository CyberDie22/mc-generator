plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.benbuzard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

application {
    mainClass.set("com.benbuzard.mcgenerator.MainKt")
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    implementation(libs.progressbar)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(libs.mappings.io)
    implementation(libs.tiny.remapper)
}

kotlin {
    jvmToolchain(21)
}