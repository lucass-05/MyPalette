package com.example.palette.features.reviews.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.ME
import com.example.palette.features.reviews.domain.model.Comentario
import com.example.palette.features.reviews.domain.model.Resena
import com.example.palette.features.reviews.domain.repository.ReviewsRepository
import kotlinx.coroutines.flow.Flow

class ReviewsRepositoryImpl(private val almacen: PaletteStore) : ReviewsRepository {

    override fun observar(logId: String): Flow<Resena?> = snapshotFlow {
        val log = almacen.log(logId) ?: return@snapshotFlow null
        val autor = almacen.user(log.userId) ?: return@snapshotFlow null
        val propia = log.userId == ME
        Resena(
            log = log,
            autor = autor,
            propia = propia,
            meGustas = almacen.meGustasDe(logId),
            meGusta = almacen.meGusta(logId),
            // los comentarios de alguien que ya no esta (cuenta borrada) no se enseñan
            comentarios = almacen.comentariosDe(logId).mapNotNull { c ->
                almacen.user(c.userId)?.let { Comentario(c.id, it, c.text, c.createdAt, puedoBorrar = propia || c.userId == ME) }
            },
        )
    }

    override fun comentar(logId: String, texto: String) = almacen.comentar(logId, texto)

    override fun borrarComentario(commentId: String) = almacen.borrarComentario(commentId)

    override fun alternarMeGusta(logId: String) = almacen.alternarMeGusta(logId)
}
