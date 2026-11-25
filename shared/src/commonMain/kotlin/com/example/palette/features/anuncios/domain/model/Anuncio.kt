package com.example.palette.features.anuncios.domain.model

import com.example.palette.core.model.Media

/** donde sale un anuncio dentro de descubrir. */
enum class Hueco {
    /** el cartel grande de arriba del todo (billboard): un estreno, con imagen y boton. */
    CARTEL,

    /** una fila de portadas como las de las recomendaciones, pero de una marca. */
    FILA,
}

/**
 * un anuncio contratado. siempre se ve marcado como "patrocinado" para que nadie lo confunda
 * con una recomendacion de verdad.
 */
data class Anuncio(
    val id: String,
    val hueco: Hueco,
    val anunciante: String,
    val titulo: String,
    val texto: String = "",
    val boton: String = "Más información",
    val enlace: String,
    // imagen ancha para el cartel (si no hay, se usa la del primer contenido)
    val imagenUrl: String? = null,
    // las portadas de la fila, o el estreno del cartel (al tocarlas se abre su ficha)
    val contenidos: List<Media> = emptyList(),
    // entre que fechas se enseña (milisegundos). null: sin limite
    val desde: Long? = null,
    val hasta: Long? = null,
    // true en los anuncios inventados del modo demostracion
    val ejemplo: Boolean = false,
)

/** lo que se enseña en descubrir: como mucho un cartel y una fila. */
data class AnunciosDeDescubrir(val cartel: Anuncio? = null, val fila: Anuncio? = null)
