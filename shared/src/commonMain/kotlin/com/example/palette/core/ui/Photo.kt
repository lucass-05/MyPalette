package com.example.palette.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** lo mismo que [avatarCuadrado] pero sin parar la interfaz mientras trabaja. devuelve null si la imagen no se puede leer. */
suspend fun prepararAvatar(foto: ByteArray): ByteArray? = withContext(Dispatchers.Default) {
    try {
        avatarCuadrado(foto)
    } catch (e: Exception) {
        null
    }
}

/**
 * la foto de cabecera del perfil: se recorta la franja central con forma 3:1, se deja en 1200 x 400
 * y se comprime en jpeg. asi pesa poco (el almacen admite hasta 1 mb) y se ve bien en cualquier ancho.
 */
fun bannerPanoramico(foto: ByteArray, ancho: Int = 1200, alto: Int = 400): ByteArray {
    val source = foto.decodeToImageBitmap()
    // el trozo mas grande de la foto que tiene la proporcion del banner
    val recorteAncho = minOf(source.width, source.height * ancho / alto)
    val recorteAlto = recorteAncho * alto / ancho
    val banner = ImageBitmap(ancho, alto)
    Canvas(banner).drawImageRect(
        image = source,
        srcOffset = IntOffset((source.width - recorteAncho) / 2, (source.height - recorteAlto) / 2),
        srcSize = IntSize(recorteAncho, recorteAlto),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(ancho, alto),
        paint = Paint().apply { filterQuality = FilterQuality.Medium },
    )
    return codificarJpeg(banner, calidad = 80)
}

/** lo mismo que [bannerPanoramico] sin parar la interfaz. devuelve null si la imagen no se puede leer. */
suspend fun prepararBanner(foto: ByteArray): ByteArray? = withContext(Dispatchers.Default) {
    try {
        bannerPanoramico(foto)
    } catch (e: Exception) {
        null
    }
}
