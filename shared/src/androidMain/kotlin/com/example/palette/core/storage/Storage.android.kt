package com.example.palette.core.storage



import android.content.Context
import android.content.SharedPreferences

private var preferencias: SharedPreferences? = null

/** hay que llamarlo desde la activity antes de pintar nada. */
fun iniciarGuardado(context: Context) {
    preferencias = context.applicationContext.getSharedPreferences("palette", Context.MODE_PRIVATE)
}

internal actual fun leerGuardado(key: String): String? = preferencias?.getString(key, null)

internal actual fun escribirGuardado(key: String, value: String) {
    preferencias?.edit()?.putString(key, value)?.apply()
}
