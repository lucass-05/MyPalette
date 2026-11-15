package com.example.palette.core.storage



private fun leerItem(key: String): String? = js("window.localStorage.getItem(key)")

private fun escribirItem(key: String, value: String): Unit = js("window.localStorage.setItem(key, value)")

internal actual fun leerGuardado(key: String): String? = leerItem(key)

internal actual fun escribirGuardado(key: String, value: String) = escribirItem(key, value)
