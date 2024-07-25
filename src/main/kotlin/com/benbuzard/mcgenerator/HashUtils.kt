package com.benbuzard.mcgenerator

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

fun File.checksum(digest: MessageDigest): String = HashUtils.checksumFile(this, digest)
fun String.checksum(digest: MessageDigest): String = HashUtils.checksumString(this, digest)

fun File.matchesChecksum(digest: MessageDigest, checksum: String): Boolean = checksum(digest) == checksum
fun String.matchesChecksum(digest: MessageDigest, checksum: String): Boolean = checksum(digest) == checksum

object HashUtils {
    const val STREAM_BUFFER_SIZE = 1024

    // TODO: implement keypair, certificate, and signature generation

    val MD2: MessageDigest get() = MessageDigest.getInstance("MD2")
    val MD5: MessageDigest get() = MessageDigest.getInstance("MD5")
    val SHA_1: MessageDigest get() = MessageDigest.getInstance("SHA-1")
    val SHA_224: MessageDigest get() = MessageDigest.getInstance("SHA-224")
    val SHA_256: MessageDigest get() = MessageDigest.getInstance("SHA-256")
    val SHA_384: MessageDigest get() = MessageDigest.getInstance("SHA-384")
    val SHA_512: MessageDigest get() = MessageDigest.getInstance("SHA-512")
    val SHA_512_224: MessageDigest get() = MessageDigest.getInstance("SHA-512/224")
    val SHA_512_256: MessageDigest get() = MessageDigest.getInstance("SHA-512/256")
    val SHA3_224: MessageDigest get() = MessageDigest.getInstance("SHA3-224")
    val SHA3_256: MessageDigest get() = MessageDigest.getInstance("SHA3-256")
    val SHA3_384: MessageDigest get() = MessageDigest.getInstance("SHA3-384")
    val SHA3_512: MessageDigest get() = MessageDigest.getInstance("SHA3-512")

    fun checksumFile(file: File, digest: MessageDigest): String {
        val fileInputStream = file.inputStream()
        val digest = updateDigest(fileInputStream, digest).digest()
        fileInputStream.close()
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun checksumString(string: String, digest: MessageDigest): String {
        val digest = digest.digest(string.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun updateDigest(fileInputStream: FileInputStream, digest: MessageDigest): MessageDigest {
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var read = fileInputStream.read(buffer, 0, STREAM_BUFFER_SIZE)
        while (read > -1) {
            digest.update(buffer, 0, read)
            read = fileInputStream.read(buffer, 0, STREAM_BUFFER_SIZE)
        }
        return digest
    }
}