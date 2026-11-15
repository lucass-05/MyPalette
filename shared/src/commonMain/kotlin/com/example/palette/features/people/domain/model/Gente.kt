package com.example.palette.features.people.domain.model

import com.example.palette.core.model.User

// el resto de usuarios de la app y a cuales de ellos sigo
data class Gente(val personas: List<User> = emptyList(), val siguiendo: Set<String> = emptySet()) {
    val amigos: List<User> get() = personas.filter { it.id in siguiendo }
}
