package com.example.palette.core.storage



// con las cookies bloqueadas safari no deja usar localStorage y lanza un error: entonces no se guarda nada, pero la app arranca
private fun leerItem(key: String): String? = js("(function () { try { return window.localStorage.getItem(key); } catch (e) { return null; } })()")

private fun escribirItem(key: String, value: String) {
    js("(function () { try { window.localStorage.setItem(key, value); } catch (e) {} })()")
}

internal actual fun leerGuardado(key: String): String? = leerItem(key)

internal actual fun escribirGuardado(key: String, value: String) = escribirItem(key, value)
