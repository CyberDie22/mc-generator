package com.benbuzard.mcgenerator

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tongfei.progressbar.ProgressBar
import java.io.File
import java.net.URI
import java.security.MessageDigest

fun File.createNewFileWithParents(shouldReplace: Boolean = false): File {
    parentFile.mkdirs()
    if (shouldReplace) {
        delete()
    }
    createNewFile()
    return this
}

suspend fun URI.download(destination: File) {
    FileDownloader.download(toString(), destination)
}

suspend fun URI.download(destination: File, digest: MessageDigest, checksum: String) {
    FileDownloader.download(toString(), destination, digest, checksum)
}

object FileDownloader {
    private val client = HttpClient(CIO)

    suspend fun download(url: String, destination: File): File {
        _download(url, destination)
        return destination
    }

    suspend fun download(url: String, destination: File, digest: MessageDigest, checksum: String): File {
        if (destination.exists() && destination.matchesChecksum(digest, checksum)) {
            println("Keeping existing file: ${destination.path} (checksum matches)")
            return destination
        }
        _downloadAndValidate(url, destination, digest, checksum)
        return destination
    }

    suspend fun downloadWithChecksumFile(url: String, destination: File, digest: MessageDigest, checksumExtension: String): File {
        val checksumUrl = "$url.$checksumExtension"
        val checksumFile = withContext(Dispatchers.IO) {
            File.createTempFile("checksum", checksumExtension)
        }
        download(checksumUrl, checksumFile)
        val checksum = checksumFile.readText().trim()
        return download(url, destination, digest, checksum)
    }

    private fun getProgressBarBuilder() = ProgressBar.builder()
        .setUpdateIntervalMillis(50)
        .setMaxRenderedLength(160)

    private suspend fun _download(url: String, destination: File) {
        client.prepareGet(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            getProgressBarBuilder().build("Downloading ${destination.path}", httpResponse.contentLength() ?: 0).use { pb ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    val bytes = packet.readBytes()
                    destination.appendBytes(bytes)
                    pb.stepBy(bytes.size.toLong())
                }
            }
        }
    }

    private suspend fun _downloadAndValidate(url: String, destination: File, digest: MessageDigest, checksum: String) {
        _download(url, destination)
        if (!destination.matchesChecksum(digest, checksum)) {
            throw Exception("Checksum mismatch while downloading $url (expected $checksum, got ${destination.checksum(digest)})")
        }
    }}