package com.example.palette.core.plataforma

// cosas del sitio donde corre la app que solo sirven para las metricas y para el modo demostracion.
// en movil y escritorio casi todo devuelve vacio: la informacion interesante (de donde viene la
// visita, el ?demo del enlace) solo existe en la web.

/** "android", "ios", "escritorio", "web" o "web-js" (la copia para iphones antiguos). */
internal expect fun nombreDePlataforma(): String

/** el valor de un parametro del enlace con el que se abrio la web (?demo, ?utm_source=tiktok...). */
internal expect fun parametroDelEnlace(nombre: String): String?

/** la pagina desde la que se llego a la web (google, tiktok, instagram...), si el navegador lo dice. */
internal expect fun paginaDeOrigen(): String?

/** idioma del dispositivo, como "es-ES". */
internal expect fun idiomaDelDispositivo(): String?

/** la version de la app que se manda con las metricas. */
internal const val VERSION_APP = "1.1.0"
