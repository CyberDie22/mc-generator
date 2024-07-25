package com.benbuzard.mcgenerator

import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.mappingio.tree.VisitableMappingTree
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import java.io.File
import java.util.jar.JarFile
import java.util.regex.Pattern

private val MAPPING_FORMATS = mapOf(
    MappingFormat.TINY_FILE to "tiny",
    MappingFormat.TINY_2_FILE to "tinyv2",
    MappingFormat.ENIGMA_FILE to "enigma",
    MappingFormat.ENIGMA_DIR to "enigma_dir",
    MappingFormat.SRG_FILE to "srg",
    MappingFormat.XSRG_FILE to "xsrg",
    MappingFormat.JAM_FILE to "jam",
    MappingFormat.CSRG_FILE to "csrg",
    MappingFormat.TSRG_FILE to "tsrg",
    MappingFormat.TSRG_2_FILE to "tsrgv2",
    MappingFormat.PROGUARD_FILE to "proguard",
    MappingFormat.RECAF_SIMPLE_FILE to "recaf_simple",
    MappingFormat.JOBF_FILE to "jobf",
)

fun MappingFormat.typeId(): String = MAPPING_FORMATS[this] ?: error("Unknown mapping format: $this")

fun File.remapJar(destination: File, mappings: Mappings, from: String, to: String) {
    println("Remapping ${this.path} to ${destination.path} using ${mappings.file.path}")
    val remapper = TinyRemapper.newRemapper()
        .withMappings(TinyUtils.createMappingProvider(mappings.tree, from, to))
        .ignoreConflicts(true)
        .rebuildSourceFilenames(true)
        .renameInvalidLocals(true)
        .invalidLvNamePattern(Pattern.compile("\\$\\$\\d+|c_[a-z]{8}"))
        .build()
    // TODO: support mixins

    OutputConsumerPath.Builder(destination.toPath()).build().use { outputConsumer ->
        try {
            outputConsumer.addNonClassFiles(destination.toPath())
            remapper.readInputs(this.toPath())
            // TODO: libraries?
            remapper.apply(outputConsumer)
            remapper.finish()
        } catch (e: Exception) {
            remapper.finish()
            throw RuntimeException("Failed to remap jar", e)
        }
    }

}

class Mappings(val file: File) {
    val tree: VisitableMappingTree = MemoryMappingTree()

    init {
        MappingReader.read(file.toPath(), tree)
    }

    private fun removeMappingIdFromName(name: String): String {
        val split = name.split("-")
        if (split.last() in MAPPING_FORMATS.values) {
            return split.dropLast(1).joinToString("-")
        }
        return name
    }

    fun autoConvertFormat(format: MappingFormat) {
        println("Converting ${file.path} to ${format.typeId()}")
        val newFile = File(file.parent, "${removeMappingIdFromName(file.nameWithoutExtension)}-${format.typeId()}.${format.fileExt}")
        write(format, newFile)
    }

    fun write(format: MappingFormat, file: File) {
        tree.accept(MappingWriter.create(file.toPath(), format))
    }
}
