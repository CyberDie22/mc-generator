package com.benbuzard.mcgenerator

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import java.io.File
import java.net.URI
import java.util.*

val VERSION_MANIFEST_URL = URI("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
val VERSION_MANIFEST_FILE = File("versions/manifest.json").createNewFileWithParents(shouldReplace = true)

object MojangManifest {
    private lateinit var manifest: ManifestData
    private val versionManifests = mutableMapOf<MinecraftVersion, VersionManifestData>()

    suspend fun getManifest(): ManifestData {
        if (!::manifest.isInitialized) {
            VERSION_MANIFEST_URL.download(VERSION_MANIFEST_FILE)
            manifest = Json.decodeFromString<ManifestData>(VERSION_MANIFEST_FILE.readText())
        }
        return manifest
    }

    suspend fun forVersion(version: MinecraftVersion): VersionManifestData {
        if (!versionManifests.containsKey(version)) {
            val manifest = getManifest()
            val versionManifestData = manifest.versions.first { MinecraftVersion(it.id) == version }
            val versionManifestUrl = versionManifestData.url
            val versionManifestFile = File("versions/$version/manifest.json").createNewFileWithParents()
            URI(versionManifestUrl).download(versionManifestFile, HashUtils.SHA_1, versionManifestData.sha1)
            versionManifests[version] = Json.decodeFromString<VersionManifestData>(versionManifestFile.readText())
        }
        return versionManifests[version]!!
    }

    suspend fun getVersions(): List<MinecraftVersion> = getManifest().versions.map { MinecraftVersion(it.id) }

    suspend fun getLatestVersion(): MinecraftVersion = MinecraftVersion(getManifest().latest.release)
}

@Serializable
data class ManifestData(
    val latest: Latest,
    val versions: List<Version>
) {
    @Serializable
    data class Latest(
        val release: String,
        val snapshot: String
    )

    @Serializable
    data class Version(
        val id: String,
        val type: String,
        val url: String,
        val time: Instant,
        val releaseTime: Instant,
        val sha1: String,
        val complianceLevel: Int
    )
}

enum class VersionType {
    Release,
    Snapshot,
}

@Serializable
data class VersionManifestData(
    val arguments: Arguments,
    val assetIndex: AssetIndex,
    val assets: String,
    val complianceLevel: Int,
    val downloads: Map<String, Download>,
    val id: String,
    val javaVersion: JavaVersion,
    val libraries: List<Library>,
    val logging: Logging,
    val mainClass: String,
    val minimumLauncherVersion: Int,
    val releaseTime: Instant,
    val time: Instant,
    val type: String,
) {
    val versionType: VersionType
        get() = when (type) {
            "release" -> VersionType.Release
            "snapshot" -> VersionType.Snapshot
            else -> throw IllegalArgumentException("Unknown version type: $type")
        }

    @kotlinx.serialization.Transient
    private lateinit var intVersion: MinecraftVersion
    val version: MinecraftVersion
        get() {
            if (!::intVersion.isInitialized) {
                intVersion = MinecraftVersion(id)
            }
            return intVersion
        }

    fun hasDownload(downloadId: String): Boolean {
        return downloads.containsKey(downloadId)
    }

    suspend fun downloadItem(downloadId: String, destination: File): File {
        val download = downloads[downloadId]!!
        URI(download.url).download(destination, HashUtils.SHA_1, download.sha1)
        return destination
    }

    fun hasMojmap(): Boolean {
        return hasDownload("client_mappings") && hasDownload("server_mappings")
    }

    suspend fun downloadClientJar(): File =
        downloadItem("client", File("versions/${id}/mojang/client.jar").createNewFileWithParents())

    suspend fun downloadServerJar(): File =
        downloadItem("server", File("versions/${id}/mojang/server.jar").createNewFileWithParents())

    suspend fun downloadClientMappings(): Optional<File> {
        if (!hasDownload("client_mappings")) {
            return Optional.empty()
        }
        return downloadItem("client_mappings", File("versions/${id}/mojmap/client_mappings-proguard.txt").createNewFileWithParents()).optional()
    }

    suspend fun downloadServerMappings(): Optional<File> {
        if (!hasDownload("server_mappings")) {
            println("has no server mappings")
            return Optional.empty()
        }
        return downloadItem("server_mappings", File("versions/${id}/mojmap/server_mappings-proguard.txt").createNewFileWithParents()).optional()
    }

    @Serializable
    data class Arguments(
        val game: JsonArray,
        val jvm: JsonArray
    )

    @Serializable
    data class AssetIndex(
        val id: String,
        val sha1: String,
        val size: Long,
        val totalSize: Long,
        val url: String,
    )

    @Serializable
    data class Download(
        val sha1: String,
        val size: Long,
        val url: String,
        val path: String? = null,
        val id: String? = null,
    )

    @Serializable
    data class JavaVersion(
        val component: String,
        val majorVersion: Int,
    )

    @Serializable
    data class Library(
        val downloads: Map<String, Download>,
        val name: String,
        val rules: List<Rule>? = null,
    )

    @Serializable
    data class Rule(
        val action: String,
        val os: OS? = null,
        val features: Map<String, Boolean>? = null,
    ) {
        @Serializable
        data class OS(
            val name: String
        )
    }

    @Serializable
    data class Logging(
        val client: Client,
    ) {
        @Serializable
        data class Client(
            val argument: String,
            val file: Download,
            val type: String,
        )
    }
}