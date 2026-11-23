package com.example.palette.features.lists.domain.repository

import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.MediaList
import com.example.palette.features.lists.domain.model.ListaVista
import kotlinx.coroutines.flow.Flow

interface ListsRepository {

    /** las listas que se ven en el perfil de esa persona (en el mio, todas). */
    fun observarListas(userId: String): Flow<List<ListaVista>>

    /** emite null si la lista ya no existe. */
    fun observarLista(listId: String): Flow<ListaVista?>

    /** mis reseñas, para elegir cuales entran en una lista de reseñas. */
    fun observarMisResenas(): Flow<List<LogEntry>>

    /** crea una lista vacia y devuelve su id. */
    fun crear(name: String, kind: ListKind): String
    fun guardar(list: MediaList)
    fun borrar(listId: String)
}
