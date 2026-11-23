package com.example.palette.features.reviews.domain.repository

import com.example.palette.features.reviews.domain.model.Resena
import kotlinx.coroutines.flow.Flow

interface ReviewsRepository {

    /** la reseña con sus comentarios. emite null si ya no existe o no se puede ver. */
    fun observar(logId: String): Flow<Resena?>

    fun comentar(logId: String, texto: String)
    fun borrarComentario(commentId: String)
    fun alternarMeGusta(logId: String)
}
