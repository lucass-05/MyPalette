package com.example.palette.features.catalog.domain.model

import com.example.palette.core.model.Extras
import com.example.palette.core.model.Media

// todo lo que enseña la ficha de un contenido
data class FichaCompleta(val media: Media, val extras: Extras, val parecidos: List<Media>)

// una fila de descubrir: "porque te gusto X" y lo que se recomienda a partir de ello
data class FilaRecomendada(val titulo: String, val origen: Media?, val contenidos: List<Media>)
