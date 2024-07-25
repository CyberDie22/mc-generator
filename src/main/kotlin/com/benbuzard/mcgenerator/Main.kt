package com.benbuzard.mcgenerator

import net.fabricmc.mappingio.format.MappingFormat
import java.io.File

suspend fun main(args: Array<String>) {
//    Security.getProviders().forEach { provider ->
//        println("-" * 50)
//        println(provider.name)
//        println(provider.info)
//        provider.services
//            .filter { it.type == "MessageDigest" }
//            .forEach { service ->
//            println("\t" + "-" * 40)
//            println("\t${service.type}")
//            println("\t${service.algorithm}")
//        }
//    }
//    return

//    val versions = manifest.getVersions().map { MinecraftVersion(it) }
    val versions = listOf(MojangManifest.getLatestVersion())

    versions.forEach { version ->
        val versionManifest = MojangManifest.forVersion(version)
        val clientJar = versionManifest.downloadClientJar()
        val serverJar = versionManifest.downloadServerJar()
        // TODO: verify signatures/checksums of jars

        // TODO: optimize mapping generation to avoid converting mappings between formats when not necessary

        // Handle Mojmap
        if (versionManifest.hasMojmap()) {
            val mojmapClientMappings = Mappings(versionManifest.downloadClientMappings().orElseThrow())
            val mojmapServerMappings = Mappings(versionManifest.downloadServerMappings().orElseThrow())

            val mojmapClientJar = File("versions/${versionManifest.id}/mojmap/client.jar")
            val mojmapServerJar = File("versions/${versionManifest.id}/mojmap/server.jar")

            clientJar.remapJar(mojmapClientJar, mojmapClientMappings, "target", "source")
            serverJar.remapJar(mojmapServerJar, mojmapServerMappings, "target", "source")
        }

        // Handle ParchmentMC
        if (ParchmentMCMappings.supportsVersion(versionManifest)) {
            val parchmentVersion = ParchmentMCMappings.getParchmentVersion(version)

            val parchmentMappings = ParchmentMCMappings.getMappings(version, parchmentVersion, checked = false)
        }
    }
}