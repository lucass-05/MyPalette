package com.example.palette

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.skia.Image

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Palette",
        // el icono de la ventana y de la barra de tareas mientras la app esta abierta.
        // el del instalador y el del acceso directo se configuran en build.gradle.kts
        icon = iconoDeLaVentana(),
        // arranca ocupando toda la pantalla
        state = rememberWindowState(placement = WindowPlacement.Maximized),
    ) {
        App()
    }
}

// lee icono.png de la carpeta resources. si no estuviera, la ventana se queda con el icono por defecto
private fun iconoDeLaVentana(): BitmapPainter? =
    object {}.javaClass.getResourceAsStream("/icono.png")?.use { fichero ->
        BitmapPainter(Image.makeFromEncoded(fichero.readBytes()).toComposeImageBitmap())
    }
