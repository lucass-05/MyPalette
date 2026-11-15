package com.example.palette.core.util


/** deja un nombre de usuario como lo admite el servidor: minusculas sin acento, numeros, punto y guion bajo. */
fun limpiarUsuario(text: String): String =
    text.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }.take(30)
