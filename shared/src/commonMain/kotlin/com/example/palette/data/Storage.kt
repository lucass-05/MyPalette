package com.example.palette.data

// almacenamiento clave-valor del dispositivo: sharedpreferences, nsuserdefaults, localstorage o un fichero.

internal expect fun readStored(key: String): String?

internal expect fun writeStored(key: String, value: String)
