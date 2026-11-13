package com.example.palette.data

private fun getItem(key: String): String? = js("window.localStorage.getItem(key)")

private fun setItem(key: String, value: String) {
    js("window.localStorage.setItem(key, value)")
}

internal actual fun readStored(key: String): String? = getItem(key)

internal actual fun writeStored(key: String, value: String) = setItem(key, value)
