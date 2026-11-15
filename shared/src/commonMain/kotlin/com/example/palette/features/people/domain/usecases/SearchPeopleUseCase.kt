package com.example.palette.features.people.domain.usecases

import com.example.palette.core.model.User
import com.example.palette.features.people.domain.model.Gente

// busca personas por nombre o por usuario. sin texto propone a quien todavia no sigo
class SearchPeopleUseCase {

    operator fun invoke(gente: Gente, consulta: String): List<User> {
        val texto = consulta.trim().removePrefix("@")
        if (texto.isEmpty()) return gente.personas.filter { it.id !in gente.siguiendo }
        return gente.personas.filter { it.username.contains(texto, ignoreCase = true) || it.name.contains(texto, ignoreCase = true) }
    }
}
