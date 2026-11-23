package com.example.palette.features.tutorial.domain.repository

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.User
import kotlinx.coroutines.flow.Flow

interface TutorialRepository {
    /** true mientras haya que enseñar el tutorial. */
    fun observarPendiente(): Flow<Boolean>

    fun marcarVisto()

    /** para verlo otra vez desde ajustes. */
    fun volverAVer()

    // lo mio de verdad, para que el tutorial se parezca a mi perfil
    fun yo(): User
    fun misLogs(): List<LogEntry>
    fun misPendientes(): List<Media>
}
