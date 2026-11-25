package com.example.palette.core.ui

import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// la imagen se guarda en fotos/palette (sin pedir permisos) y se abre el menu de compartir con ella
@Composable
actual fun recordarCompartirImagen(): (jpeg: ByteArray, nombre: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { jpeg, nombre ->
            val datos = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$nombre.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Palette")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, datos)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(jpeg) }
                val enviar = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(enviar, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

actual val compartirImagenDisponible: Boolean = true
