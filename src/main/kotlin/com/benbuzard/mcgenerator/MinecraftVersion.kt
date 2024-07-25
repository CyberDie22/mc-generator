package com.benbuzard.mcgenerator

data class MinecraftVersion(val major: Int, val minor: Int, val patch: Int) {
    constructor(version: String) : this(
        version.split(".").getOrNull(0)?.toIntOrNull() ?: 0,
        version.split(".").getOrNull(1)?.toIntOrNull() ?: 0,
        version.split(".").getOrNull(2)?.toIntOrNull() ?: 0
    )

    operator fun compareTo(other: MinecraftVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    override fun toString(): String = "$major.$minor${if (patch == 0) "" else ".$patch"}"
}
