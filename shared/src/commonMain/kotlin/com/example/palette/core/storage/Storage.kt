package com.example.palette.core.storage


// expect/actual es la forma de kotlin multiplatform de pedir algo distinto a cada plataforma:
// aqui se declara que existe la funcion (expect) y cada carpeta androidMain, iosMain, jvmMain,
// jsMain y wasmJsMain da su version (actual). el codigo comun solo ve esta declaracion.

// almacenamiento clave-valor del dispositivo: sharedpreferences, nsuserdefaults, localstorage o un fichero.

internal expect fun leerGuardado(key: String): String?

internal expect fun escribirGuardado(key: String, value: String)
