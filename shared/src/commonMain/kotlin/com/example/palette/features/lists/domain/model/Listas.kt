package com.example.palette.features.lists.domain.model

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.MediaList

// una lista lista para pintarse: en las de reseñas, los ids ya vienen cambiados por los logs.
// si una reseña se ha borrado desaparece de resenas aunque su id siga en la lista
data class ListaVista(
    val lista: MediaList,
    val propia: Boolean,
    val resenas: List<LogEntry> = emptyList(),
)
