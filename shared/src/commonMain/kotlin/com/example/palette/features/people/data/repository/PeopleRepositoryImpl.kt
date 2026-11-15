package com.example.palette.features.people.data.repository

import androidx.compose.runtime.snapshotFlow
import com.example.palette.core.data.PaletteStore
import com.example.palette.features.people.domain.model.Gente
import com.example.palette.features.people.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow

class PeopleRepositoryImpl(private val almacen: PaletteStore) : PeopleRepository {

    override fun observar(): Flow<Gente> = snapshotFlow { Gente(almacen.estado.people, almacen.estado.following) }

    override fun alternarSeguir(userId: String) = almacen.alternarSeguir(userId)
}
