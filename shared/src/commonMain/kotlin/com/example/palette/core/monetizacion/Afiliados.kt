package com.example.palette.core.monetizacion

import com.example.palette.core.model.Media
import com.example.palette.core.model.MediaType
import io.ktor.http.encodeURLParameter

/**
 * los enlaces que llevan a comprar, escuchar o ver un contenido fuera de la app.
 *
 * cada programa de afiliados da un id al darse de alta. mientras el id este vacio el enlace funciona
 * igual, solo que sin cobrar nada. al rellenarlo, cada visita o compra que venga de palette cuenta.
 */
object Afiliados {
    // apple services performance partners (apple music, apple tv, libros de apple): parametro at
    const val APPLE = ""

    // amazon afiliados de amazon.es (libros, ediciones fisicas, prime video): parametro tag
    const val AMAZON = ""

    // instant gaming (claves de juegos para pc y consola): parametro igr
    const val INSTANT_GAMING = ""

    /** los botones de la ficha de un contenido. vacio si no hay ningun sitio al que mandar. */
    fun ofertas(media: Media): List<Oferta> {
        val busqueda = listOfNotNull(media.title, media.creator.takeIf { media.type == MediaType.BOOK }).joinToString(" ").encodeURLParameter()
        return when (media.type) {
            // el enlace de itunes ya lleva a apple music; solo falta el id
            MediaType.SONG, MediaType.ALBUM -> listOfNotNull(
                media.link?.let { Oferta("Apple Music", "Escuchar en Apple Music", conParametro(it, "at", APPLE, "ct" to "palette")) },
            )
            MediaType.BOOK -> listOf(
                Oferta("Amazon", "Comprar en Amazon", conParametro("https://www.amazon.es/s?k=$busqueda&i=stripbooks", "tag", AMAZON)),
            )
            MediaType.GAME -> listOf(
                Oferta("Instant Gaming", "Comprar en Instant Gaming", conParametro("https://www.instant-gaming.com/es/busquedas/?q=$busqueda", "igr", INSTANT_GAMING)),
            )
            MediaType.MOVIE, MediaType.SERIES -> listOf(
                Oferta("Amazon", "Comprar en Amazon", conParametro("https://www.amazon.es/s?k=$busqueda&i=dvd", "tag", AMAZON)),
            )
        }
    }

    /**
     * a donde lleva tocar una plataforma de "dónde verlo". si es una que se conoce, a su buscador;
     * si no, null y se usa la pagina general de justwatch.
     */
    fun enlaceDePlataforma(plataforma: String, media: Media): String? {
        val titulo = media.title.encodeURLParameter()
        val nombre = plataforma.lowercase()
        return when {
            "netflix" in nombre -> "https://www.netflix.com/search?q=$titulo"
            "disney" in nombre -> "https://www.disneyplus.com/es-es/search?q=$titulo"
            "prime" in nombre || "amazon" in nombre -> conParametro("https://www.primevideo.com/search?phrase=$titulo", "tag", AMAZON)
            "apple" in nombre -> conParametro("https://tv.apple.com/es/search?term=$titulo", "at", APPLE)
            "max" == nombre || "hbo" in nombre -> "https://play.max.com/search?q=$titulo"
            "movistar" in nombre -> "https://ver.movistarplus.es/busqueda/?q=$titulo"
            "filmin" in nombre -> "https://www.filmin.es/buscador?q=$titulo"
            "skyshowtime" in nombre -> "https://www.skyshowtime.com/es/search?q=$titulo"
            else -> null
        }
    }

    /** true si ese enlace lleva id de afiliado (para las metricas: cuantos clics cobran). */
    fun esDeAfiliado(enlace: String): Boolean =
        listOf("at" to APPLE, "tag" to AMAZON, "igr" to INSTANT_GAMING).any { (clave, id) -> id.isNotEmpty() && "$clave=$id" in enlace }

    // añade el id al enlace solo si hay id
    private fun conParametro(enlace: String, clave: String, id: String, vararg extra: Pair<String, String>): String {
        if (id.isEmpty()) return enlace
        val separador = if ('?' in enlace) "&" else "?"
        return enlace + separador + (listOf(clave to id) + extra).joinToString("&") { (k, v) -> "$k=$v" }
    }
}

/** un boton de la ficha: la tienda, lo que pone y a donde lleva. */
data class Oferta(val tienda: String, val texto: String, val enlace: String)
