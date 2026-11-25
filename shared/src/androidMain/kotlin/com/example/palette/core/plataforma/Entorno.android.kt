package com.example.palette.core.plataforma

import java.util.Locale

internal actual fun nombreDePlataforma(): String = "android"

internal actual fun parametroDelEnlace(nombre: String): String? = null

internal actual fun paginaDeOrigen(): String? = null

internal actual fun idiomaDelDispositivo(): String? = Locale.getDefault().toLanguageTag()
