package com.example.palette.core.storage

// los correos de supabase (confirmar la cuenta, contraseña olvidada) llevan un enlace que abre la web
// de la app con datos detras de la almohadilla: #access_token=...&refresh_token=...&type=recovery
// solo existe en web. en las demas plataformas no hay direccion que leer y devuelve null.

/** lo que hay detras de la # en la direccion con la que se ha abierto la app. se lee una sola vez. */
internal expect fun leerEnlaceDeEntrada(): String?
