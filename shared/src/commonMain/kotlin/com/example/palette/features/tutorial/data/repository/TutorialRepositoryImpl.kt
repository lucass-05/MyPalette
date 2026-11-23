package com.example.palette.features.tutorial.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.User
import com.example.palette.features.tutorial.domain.repository.TutorialRepository
import kotlinx.coroutines.flow.Flow

class TutorialRepositoryImpl(private val almacen: PaletteStore) : TutorialRepository {

    override fun observarPendiente(): Flow<Boolean> = snapshotFlow { !almacen.estado.tutorialVisto }

    override fun marcarVisto() = almacen.ponerTutorialVisto(true)

    override fun volverAVer() = almacen.ponerTutorialVisto(false)

    override fun yo(): User = almacen.me

    // los ocultos no salen: en el tutorial se ve el perfil como lo ven los demas
    override fun misLogs(): List<LogEntry> = almacen.estado.logs.filter { !it.hidden }

    override fun misPendientes(): List<Media> = almacen.estado.watchlist
}
