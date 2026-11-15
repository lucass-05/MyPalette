package com.example.palette.features.people.domain.repository

import com.example.palette.features.people.domain.model.Gente
import kotlinx.coroutines.flow.Flow

interface PeopleRepository {

    fun observar(): Flow<Gente>

    /** empieza a seguir a esa persona o deja de seguirla. */
    fun alternarSeguir(userId: String)
}
