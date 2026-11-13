package com.example.palette.data

import android.content.Context
import android.content.SharedPreferences

private var prefs: SharedPreferences? = null

/** hay que llamarlo desde la activity antes de pintar nada. */
fun initStorage(context: Context) {
    prefs = context.applicationContext.getSharedPreferences("palette", Context.MODE_PRIVATE)
}

internal actual fun readStored(key: String): String? = prefs?.getString(key, null)

internal actual fun writeStored(key: String, value: String) {
    prefs?.edit()?.putString(key, value)?.apply()
}
