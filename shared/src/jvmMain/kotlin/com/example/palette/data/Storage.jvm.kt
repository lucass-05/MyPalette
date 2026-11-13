package com.example.palette.data

import java.io.File

// en escritorio se guarda en ~/.palette
private fun ficheroDe(key: String) = File(File(System.getProperty("user.home"), ".palette"), "$key.json")

internal actual fun leerGuardado(key: String): String? = ficheroDe(key).takeIf { it.exists() }?.readText()

internal actual fun escribirGuardado(key: String, value: String) {
    val file = ficheroDe(key)
    file.parentFile.mkdirs()
    file.writeText(value)
}
