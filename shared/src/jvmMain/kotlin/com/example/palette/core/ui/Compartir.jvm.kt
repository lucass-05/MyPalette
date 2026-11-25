package com.example.palette.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.io.File

// en el ordenador se guarda en descargas y se abre la carpeta para arrastrarla donde se quiera
@Composable
actual fun recordarCompartirImagen(): (jpeg: ByteArray, nombre: String) -> Unit = remember {
    { jpeg, nombre ->
        val carpeta = File(System.getProperty("user.home"), "Downloads").takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))
        val archivo = File(carpeta, "$nombre.jpg")
        archivo.writeBytes(jpeg)
        runCatching { Desktop.getDesktop().open(carpeta) }
    }
}

actual val compartirImagenDisponible: Boolean = true
