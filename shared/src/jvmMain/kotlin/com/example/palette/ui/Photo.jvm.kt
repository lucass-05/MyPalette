package com.example.palette.ui

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
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val report by rememberUpdatedState(onPicked)
    return remember {
        {
            // el dialogo de archivos del sistema; en windows el filtro de tipos se da como patron en el nombre
            val dialog = FileDialog(null as Frame?, "Elegir foto", FileDialog.LOAD)
            dialog.file = "*.jpg;*.jpeg;*.png;*.webp"
            dialog.isVisible = true
            val folder = dialog.directory
            val name = dialog.file
            if (folder != null && name != null) report(File(folder, name).readBytes())
        }
    }
}

internal actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray =
    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: ByteArray(0)
