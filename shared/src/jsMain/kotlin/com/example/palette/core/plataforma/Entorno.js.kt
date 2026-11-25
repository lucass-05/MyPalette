package com.example.palette.core.plataforma

private fun leerParametro(nombre: String): String? = js("new URLSearchParams(window.location.search).get(nombre)")

private fun leerOrigen(): String = js("document.referrer")

private fun leerIdioma(): String = js("navigator.language || ''")

internal actual fun nombreDePlataforma(): String = "web-js"

internal actual fun parametroDelEnlace(nombre: String): String? = leerParametro(nombre)

// vacio cuando se entra escribiendo la direccion o desde una app que no lo dice
internal actual fun paginaDeOrigen(): String? = leerOrigen().ifBlank { null }

internal actual fun idiomaDelDispositivo(): String? = leerIdioma().ifBlank { null }
