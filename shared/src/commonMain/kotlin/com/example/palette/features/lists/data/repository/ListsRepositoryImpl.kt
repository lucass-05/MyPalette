package com.example.palette.features.lists.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.ListKind
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.ME
import com.example.palette.core.model.MediaList
import com.example.palette.features.lists.domain.model.ListaVista
import com.example.palette.features.lists.domain.repository.ListsRepository
import kotlinx.coroutines.flow.Flow

class ListsRepositoryImpl(private val almacen: PaletteStore) : ListsRepository {

    override fun observarListas(userId: String): Flow<List<ListaVista>> = snapshotFlow {
        almacen.listasDe(userId).map { preparar(it) }
    }

    override fun observarLista(listId: String): Flow<ListaVista?> = snapshotFlow {
        almacen.lista(listId)?.let { preparar(it) }
    }

    override fun observarMisResenas(): Flow<List<LogEntry>> = snapshotFlow {
        almacen.estado.logs.sortedByDescending { it.createdAt }
    }

    override fun crear(name: String, kind: ListKind): String = almacen.crearLista(name, kind).id

    override fun guardar(list: MediaList) = almacen.guardarLista(list)

    override fun borrar(listId: String) = almacen.borrarLista(listId)

    private fun preparar(list: MediaList): ListaVista {
        // en mi perfil salen tambien mis reseñas ocultas; en el de otra persona, no
        val resenas = if (list.kind == ListKind.REVIEWS) {
            list.logIds.mapNotNull { almacen.log(it) }.filter { list.userId == ME || !it.hidden }
        } else {
            emptyList()
        }
        return ListaVista(list, list.userId == ME, resenas)
    }
}
