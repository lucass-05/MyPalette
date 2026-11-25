package com.example.palette.core.plataforma

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

internal actual fun nombreDePlataforma(): String = "ios"

internal actual fun parametroDelEnlace(nombre: String): String? = null

internal actual fun paginaDeOrigen(): String? = null

internal actual fun idiomaDelDispositivo(): String? = NSLocale.currentLocale.localeIdentifier
