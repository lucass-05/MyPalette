package com.example.palette.features.reviews.domain.model

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.User

// un comentario con quien lo escribio. puedoBorrar: es mio, o la reseña es mia
data class Comentario(
    val id: String,
    val autor: User,
    val texto: String,
    val at: Long,
    val puedoBorrar: Boolean,
)

// una reseña abierta: el log, su autor, cuantos me gusta tiene y sus comentarios
data class Resena(
    val log: LogEntry,
    val autor: User,
    val propia: Boolean,
    val meGustas: Int,
    val meGusta: Boolean,
    val comentarios: List<Comentario>,
)
