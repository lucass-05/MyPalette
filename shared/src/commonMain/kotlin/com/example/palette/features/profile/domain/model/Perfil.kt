package com.example.palette.features.profile.domain.model

import com.example.palette.core.model.LogEntry
import com.example.palette.core.model.Media
import com.example.palette.core.model.User

// todo lo que enseña la pantalla de perfil de alguien
data class Perfil(
    val user: User,
    // true si es mi propio perfil
    val propio: Boolean,
    // sus logs sin ordenar. en mi perfil vienen tambien los ocultos
    val logs: List<LogEntry>,
    // null si esa persona tiene sus pendientes en privado
    val pendientes: List<Media>?,
    val loSigo: Boolean,
    // solo en mi perfil: los que me siguen y a los que sigo
    val amigos: List<User> = emptyList(),
) {
    /** cuantas reseñas ha publicado (las ocultas no cuentan). */
    val publicaciones: Int get() = logs.count { !it.hidden }
}
