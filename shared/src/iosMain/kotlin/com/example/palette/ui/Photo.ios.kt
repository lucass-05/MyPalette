package com.example.palette.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// pendiente: en ios falta abrir la galeria (phpickerviewcontroller). de momento el boton no hace nada.
@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): () -> Unit = {}

internal actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray =
    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: ByteArray(0)
