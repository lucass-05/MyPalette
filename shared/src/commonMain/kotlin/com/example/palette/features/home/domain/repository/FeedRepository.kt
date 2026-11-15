package com.example.palette.features.home.domain.repository

import com.example.palette.features.home.domain.model.Feed
import com.example.palette.features.home.domain.model.Historia
import kotlinx.coroutines.flow.Flow

interface FeedRepository {

    fun observar(): Flow<Feed>

    /** la historia de esa persona tal y como esta ahora mismo, o null si no tiene. */
    fun historiaDe(userId: String): Historia?

    fun alternarMeGusta(logId: String)

    /** apunta que ya he visto la historia de esa persona hasta el log de esa fecha. */
    fun marcarHistoriaVista(userId: String, hasta: Long)
}
