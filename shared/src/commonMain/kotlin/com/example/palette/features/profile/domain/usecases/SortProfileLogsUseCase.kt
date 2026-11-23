package com.example.palette.features.profile.domain.usecases

import com.example.palette.core.model.LogEntry

// el orden de favoritos (la pagina y el bloque): fijados primero, luego de mas a menos estrellas
// y, con la misma nota, lo mas reciente antes
class SortProfileLogsUseCase {

    operator fun invoke(logs: List<LogEntry>): List<LogEntry> =
        logs.sortedWith(compareByDescending<LogEntry> { it.pinned }.thenByDescending { it.stars }.thenByDescending { it.createdAt })
}
