package com.example.palette.features.home.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.features.home.domain.model.Feed
import com.example.palette.features.home.domain.model.Historia
import com.example.palette.features.home.domain.model.Publicacion
import com.example.palette.features.home.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

class FeedRepositoryImpl(private val almacen: PaletteStore) : FeedRepository {

    override fun observar(): Flow<Feed> = snapshotFlow {
        Feed(
            publicaciones = almacen.novedades.mapNotNull { log ->
                val autor = almacen.user(log.userId) ?: return@mapNotNull null
                Publicacion(
                    autor, log, almacen.meGusta(log.id), almacen.enPendientes(log.media.id),
                    meGustas = almacen.meGustasDe(log.id),
                    comentarios = almacen.comentariosDe(log.id).size,
                )
            },
            historias = almacen.historias.mapNotNull { historiaDe(it.id) },
            sigoAAlguien = almacen.amigos.isNotEmpty(),
            sinConexion = almacen.sinConexion,
        )
    }

    override fun historiaDe(userId: String): Historia? {
        val user = almacen.user(userId) ?: return null
        val logs = almacen.historiaDe(userId)
        return if (logs.isEmpty()) null else Historia(user, logs, almacen.tieneHistoriaSinVer(userId))
    }

    override fun alternarMeGusta(logId: String) = almacen.alternarMeGusta(logId)

    override fun marcarHistoriaVista(userId: String, hasta: Long) = almacen.marcarHistoriaVista(userId, hasta)
}
