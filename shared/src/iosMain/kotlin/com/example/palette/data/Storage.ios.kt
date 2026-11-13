package com.example.palette.data

import platform.Foundation.NSUserDefaults

internal actual fun leerGuardado(key: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(key)

internal actual fun escribirGuardado(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
}
