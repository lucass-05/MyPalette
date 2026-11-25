package com.example.palette.core.ui

import androidx.compose.runtime.Composable

// pendiente: en ios nativo falta abrir uiactivityviewcontroller. en el iphone se usa la web, que si comparte
@Composable
actual fun recordarCompartirImagen(): (jpeg: ByteArray, nombre: String) -> Unit = { _, _ -> }

actual val compartirImagenDisponible: Boolean = false
