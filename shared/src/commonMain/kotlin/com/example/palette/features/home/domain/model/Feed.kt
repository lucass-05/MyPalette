package com.example.palette.features.home.domain.model

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.User

// un log de alguien a quien sigo tal y como sale en inicio
data class Publicacion(val autor: User, val log: LogEntry, val meGusta: Boolean, val enPendientes: Boolean)

// los logs de los ultimos dias de un amigo, para verlos uno detras de otro
data class Historia(val user: User, val logs: List<LogEntry>, val sinVer: Boolean)

data class Feed(
    val publicaciones: List<Publicacion> = emptyList(),
    val historias: List<Historia> = emptyList(),
    val sigoAAlguien: Boolean = false,
    // true cuando el ultimo intento de hablar con el servidor ha fallado
    val sinConexion: Boolean = false,
)
