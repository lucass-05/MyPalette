package com.example.palette.core.ui



import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

@Composable
actual fun recordarSelectorDeFotos(alElegir: (ByteArray) -> Unit): () -> Unit {
    val avisar by rememberUpdatedState(alElegir)
    return remember {
        {
            // el dialogo de archivos del sistema; en windows el filtro de tipos se da como patron en el nombre
            val dialogo = FileDialog(null as Frame?, "Elegir foto", FileDialog.LOAD)
            dialogo.file = "*.jpg;*.jpeg;*.png;*.webp"
            dialogo.isVisible = true
            val carpeta = dialogo.directory
            val name = dialogo.file
            if (carpeta != null && name != null) avisar(File(carpeta, name).readBytes())
        }
    }
}

internal actual fun codificarJpeg(image: ImageBitmap, calidad: Int): ByteArray =
    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, calidad)?.bytes ?: ByteArray(0)
