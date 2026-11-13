package com.example.palette.data

import platform.Foundation.NSUserDefaults

internal actual fun readStored(key: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(key)

internal actual fun writeStored(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
}
