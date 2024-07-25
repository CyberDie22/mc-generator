package com.benbuzard.mcgenerator

operator fun String.times(n: Int): String {
    return (0 until n).joinToString("") { this }
}