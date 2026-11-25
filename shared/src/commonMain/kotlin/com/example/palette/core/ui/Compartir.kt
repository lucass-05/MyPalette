package com.example.palette.core.ui

import androidx.compose.runtime.Composable

/**
 * devuelve una funcion que comparte una imagen jpeg:
 * - en el movil abre el menu de compartir del sistema (instagram, whatsapp, tiktok...)
 * - en la web tambien, si el navegador sabe; si no, la descarga
 * - en el ordenador la guarda en descargas
 */
@Composable
expect fun recordarCompartirImagen(): (jpeg: ByteArray, nombre: String) -> Unit

/** false donde todavia no se sabe compartir imagenes (ios nativo): ahi no sale el boton. */
expect val compartirImagenDisponible: Boolean
