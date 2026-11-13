package com.example.palette.data

import java.io.File

// en escritorio se guarda en ~/.palette
private fun fileFor(key: String) = File(File(System.getProperty("user.home"), ".palette"), "$key.json")

internal actual fun readStored(key: String): String? = fileFor(key).takeIf { it.exists() }?.readText()

internal actual fun writeStored(key: String, value: String) {
    val file = fileFor(key)
    file.parentFile.mkdirs()
    file.writeText(value)
}
