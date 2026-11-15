package com.example.palette.features.library.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.Media
import com.example.palette.features.library.domain.model.Biblioteca
import com.example.palette.features.library.domain.model.LogDeAmigo
import com.example.palette.features.library.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow

// los datos siguen guardados en el almacen (copia local + supabase); este repositorio es la puerta
// por la que el resto de la app los pide, sin saber donde estan
class LibraryRepositoryImpl(private val almacen: PaletteStore) : LibraryRepository {

    override fun observar(): Flow<Biblioteca> = snapshotFlow {
        val estado = almacen.estado
        Biblioteca(
            logs = estado.logs,
            pendientes = estado.watchlist,
            logsDeAmigos = estado.peopleLogs
                .filter { it.userId in estado.following && !it.hidden }
                .mapNotNull { log -> almacen.user(log.userId)?.let { LogDeAmigo(it, log) } },
        )
    }

    override fun guardarLog(media: Media, stars: Float, review: String) = almacen.guardarLog(media, stars, review)

    override fun borrarLog(logId: String) = almacen.borrarLog(logId)

    override fun alternarFijado(logId: String) = almacen.alternarFijado(logId)

    override fun alternarOculto(logId: String) = almacen.alternarOculto(logId)

    override fun alternarPendiente(media: Media) = almacen.alternarPendiente(media)
}
