package com.example.palette.features.library.domain.model

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.User

// un log de alguien a quien sigo, junto con quien lo escribio
data class LogDeAmigo(val autor: User, val log: LogEntry)

// lo que hay registrado: mis logs, mis pendientes y los logs de la gente a la que sigo
data class Biblioteca(
    val logs: List<LogEntry> = emptyList(),
    val pendientes: List<Media> = emptyList(),
    val logsDeAmigos: List<LogDeAmigo> = emptyList(),
) {
    // ids de todo lo que ya tengo, sea registrado o pendiente
    val idsQueTengo: Set<String> get() = (logs.map { it.media.id } + pendientes.map { it.id }).toSet()
}
