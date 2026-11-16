package com.example.palette.core.storage

private fun leerFragmento(): String = js("window.location.hash")

// se quita de la direccion para que no se quede en el historial ni se vuelva a usar al recargar
private fun borrarFragmento() {
    js("window.history.replaceState(null, '', window.location.pathname + window.location.search)")
}

internal actual fun leerEnlaceDeEntrada(): String? {
    val fragmento = leerFragmento().removePrefix("#")
    if (fragmento.isEmpty()) return null
    borrarFragmento()
    return fragmento
}
