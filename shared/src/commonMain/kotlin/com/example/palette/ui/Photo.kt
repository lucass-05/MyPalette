package com.example.palette.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * deja preparado el selector de fotos del dispositivo (galeria en el movil, explorador de archivos
 * en escritorio y web) y devuelve la funcion que lo abre. [onPicked] recibe la imagen tal cual, en bytes.
 */
@Composable
expect fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): () -> Unit

internal expect fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray

/**
 * convierte cualquier foto en la que se sube como avatar: se recorta el cuadrado central (1:1),
 * se reduce a [side] pixeles de lado y se comprime en jpeg. asi ninguna pasa de unos 60 kb.
 */
fun squareAvatar(photo: ByteArray, side: Int = 512): ByteArray {
    val source = photo.decodeToImageBitmap()
    val crop = minOf(source.width, source.height)
    val square = ImageBitmap(side, side)
    Canvas(square).drawImageRect(
        image = source,
        srcOffset = IntOffset((source.width - crop) / 2, (source.height - crop) / 2),
        srcSize = IntSize(crop, crop),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(side, side),
        paint = Paint().apply { filterQuality = FilterQuality.Medium },
    )
    return encodeJpeg(square, quality = 85)
}
