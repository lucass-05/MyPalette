package com.example.palette.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val report by rememberUpdatedState(onPicked)
    // el selector de fotos del sistema: no hace falta pedir permiso de almacenamiento
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) readUpright(context, uri)?.let(report)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** lee la foto y, si la camara la guardo girada (dato exif), la devuelve ya derecha. */
private fun readUpright(context: Context, uri: Uri): ByteArray? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val turn = when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bytes
    }
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
    val upright = Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { postRotate(turn) }, true)
    return ByteArrayOutputStream().also { upright.compress(Bitmap.CompressFormat.JPEG, 92, it) }.toByteArray()
}

internal actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray =
    ByteArrayOutputStream().also { image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
