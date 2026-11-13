package com.example.palette.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// abre el selector de archivos del navegador y devuelve la imagen elegida en base64,
// que es la forma sencilla de pasar bytes de javascript a kotlin.
private fun abrirSelector(onBase64: (String) -> Unit): Unit = js(
    """{
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.onchange = () => {
            const file = input.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = () => onBase64(reader.result.split(',')[1]);
            reader.readAsDataURL(file);
        };
        input.click();
    }"""
)

@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun recordarSelectorDeFotos(alElegir: (ByteArray) -> Unit): () -> Unit {
    val avisar by rememberUpdatedState(alElegir)
    return remember { { abrirSelector { codificada -> avisar(Base64.decode(codificada)) } } }
}

internal actual fun codificarJpeg(image: ImageBitmap, calidad: Int): ByteArray =
    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, calidad)?.bytes ?: ByteArray(0)
