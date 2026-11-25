package com.example.palette.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// en el movil el navegador abre su menu de compartir (con instagram, whatsapp...); en el ordenador, se descarga.
// los bytes pasan a javascript en base64, igual que al elegir una foto
private fun compartirBase64(base64: String, nombre: String) {
    js(
        """
        var binario = atob(base64);
        var bytes = new Uint8Array(binario.length);
        for (var i = 0; i < binario.length; i++) bytes[i] = binario.charCodeAt(i);
        var archivo = new File([bytes], nombre + ".jpg", { type: "image/jpeg" });
        var descargar = function () {
            var url = URL.createObjectURL(archivo);
            var enlace = document.createElement("a");
            enlace.href = url;
            enlace.download = nombre + ".jpg";
            document.body.appendChild(enlace);
            enlace.click();
            enlace.remove();
            setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
        };
        if (navigator.canShare && navigator.canShare({ files: [archivo] })) navigator.share({ files: [archivo] }).catch(function (e) { if (e.name !== "AbortError") descargar(); });
        else descargar();
        """
    )
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun recordarCompartirImagen(): (jpeg: ByteArray, nombre: String) -> Unit = remember {
    { jpeg, nombre -> compartirBase64(Base64.encode(jpeg), nombre) }
}

actual val compartirImagenDisponible: Boolean = true
