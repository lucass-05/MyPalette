package com.example.palette.features.library.domain.repository

import com.example.palette.core.model.Media
import com.example.palette.features.library.domain.model.Biblioteca
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

    // un Flow es un valor que va cambiando: quien lo "colecciona" recibe la biblioteca actual
    // y otra vez cada vez que cambie (un log nuevo, algo guardado en pendientes...)
    fun observar(): Flow<Biblioteca>

    /** crea mi log de ese contenido o cambia el que ya tenia. */
    fun guardarLog(media: Media, stars: Float, review: String)
    fun borrarLog(logId: String)
    fun alternarFijado(logId: String)
    fun alternarOculto(logId: String)

    /** guarda un contenido en pendientes o lo quita si ya estaba. */
    fun alternarPendiente(media: Media)
}
