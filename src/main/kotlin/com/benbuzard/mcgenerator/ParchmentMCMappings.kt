package com.benbuzard.mcgenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

object ParchmentMCMappings {
    fun supportsVersion(versionManifestData: VersionManifestData): Boolean {
        if (versionManifestData.versionType != VersionType.Release) return false // Parchment only supports release versions

        val skippedVersions = listOf(
            MinecraftVersion("1.18"), MinecraftVersion("1.19"), MinecraftVersion("1.19.1"),
            MinecraftVersion("1.20")
        )
        if (skippedVersions.contains(versionManifestData.version)) return false // Parchment has skipped versions

        return versionManifestData.version >= MinecraftVersion("1.16.5") // Parchment only supports versions >=1.16.5
    }

    suspend fun getParchmentVersion(minecraftVersion: MinecraftVersion): String {
        val baseUrl = "https://ldtteam.jfrog.io/artifactory/parchmentmc-public/org/parchmentmc/data/parchment-$minecraftVersion"
        // TODO: hash verification fails for some reason
        val mavenMetadata = FileDownloader.download(
            "$baseUrl/maven-metadata.xml",
            File("versions/$minecraftVersion/parchment/maven-metadata.xml").createNewFileWithParents(),
//            HashUtils.SHA_512,
//            "sha512"
        )
        return mavenMetadata.readText().substringAfter("<latest>").substringBefore("</latest>")
    }

    suspend fun getMappings(minecraftVersion: MinecraftVersion, parchmentVersion: String, checked: Boolean): Mappings {
        val baseUrl = "https://ldtteam.jfrog.io/artifactory/parchmentmc-public/org/parchmentmc/data/parchment-$minecraftVersion"


        val mappingsZip = FileDownloader.downloadWithChecksumFile(
            "$baseUrl/$parchmentVersion/parchment-$minecraftVersion-$parchmentVersion${if (checked) "-checked" else ""}.zip",
            File("versions/$minecraftVersion/parchment/mappings${if (checked) "-checked" else ""}.zip"),
            HashUtils.SHA_512,
            "sha512"
        )

        println("Extracting mappings from $mappingsZip")

        val mappingsZipFile = withContext(Dispatchers.IO) {
            ZipFile(mappingsZip)
        }
        val parchmentMappingsJson = withContext(Dispatchers.IO) {
            mappingsZipFile.getInputStream(
                mappingsZipFile.getEntry("parchment.json")
            )
        }.reader().readText()

        val parchmentMappingsFile = File("versions/$minecraftVersion/parchment/mappings${if (checked) "-checked" else ""}.json").createNewFileWithParents()
        parchmentMappingsFile.writeText(parchmentMappingsJson)

        TODO()
    }
}