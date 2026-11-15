package com.example.palette.features.profile.domain.usecases

import com.example.palette.core.model.LogEntry

// las dos maneras de ordenar la paleta de un perfil
class SortProfileLogsUseCase {

    operator fun invoke(logs: List<LogEntry>, recientesPrimero: Boolean): List<LogEntry> =
        // como diario: lo ultimo registrado arriba, sin tener en cuenta fijados ni nota
        if (recientesPrimero) logs.sortedByDescending { it.createdAt }
        // favoritos: fijados primero y luego de mas a menos estrellas
        else logs.sortedWith(compareByDescending<LogEntry> { it.pinned }.thenByDescending { it.stars }.thenByDescending { it.createdAt })
}
