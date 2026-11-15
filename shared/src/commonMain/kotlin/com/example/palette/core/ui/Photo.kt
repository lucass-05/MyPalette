package com.example.palette.core.ui



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
expect fun recordarSelectorDeFotos(alElegir: (ByteArray) -> Unit): () -> Unit

internal expect fun codificarJpeg(image: ImageBitmap, calidad: Int): ByteArray

/**
 * convierte cualquier foto en la que se sube como avatar: se recorta el cuadrado central (1:1),
 * se reduce a [side] pixeles de lado y se comprime en jpeg. asi ninguna pasa de unos 60 kb.
 */
fun avatarCuadrado(foto: ByteArray, lado: Int = 512): ByteArray {
    val source = foto.decodeToImageBitmap()
    val recorte = minOf(source.width, source.height)
    val cuadrada = ImageBitmap(lado, lado)
    Canvas(cuadrada).drawImageRect(
        image = source,
        srcOffset = IntOffset((source.width - recorte) / 2, (source.height - recorte) / 2),
        srcSize = IntSize(recorte, recorte),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(lado, lado),
        paint = Paint().apply { filterQuality = FilterQuality.Medium },
    )
    return codificarJpeg(cuadrada, calidad = 85)
}
